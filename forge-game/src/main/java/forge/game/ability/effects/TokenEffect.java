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
package forge.game.ability.effects;

import org.apache.commons.lang3.mutable.MutableBoolean;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.card.token.TokenInfo;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class TokenEffect extends TokenEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getDescription();
    }

    public Card loadTokenPrototype(SpellAbility sa) {
        if (!sa.hasParam("TokenScript")) {
            return null;
        }

        final Card result = TokenInfo.getProtoType(sa.getParam("TokenScript"), sa);

        if (result == null) {
            throw new RuntimeException("don't find Token for TokenScript: " + sa.getParam("TokenScript"));
        }

        return result;
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        // linked Abilities, if it needs chosen values, but nothing is chosen, no token can be created
        if (sa.hasParam("TokenTypes")) {
            if (sa.getParam("TokenTypes").contains("ChosenType") && !host.hasChosenType()) {
                return;
            }
        }
        if (sa.hasParam("TokenColors")) {
            if (sa.getParam("TokenColors").contains("ChosenColor") && !host.hasChosenColor()) {
                return;
            }
        }

        Card prototype = loadTokenPrototype(sa);

        final int finalAmount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("TokenAmount", "1"), sa);

        boolean useZoneTable = true;
        CardZoneTable triggerList = sa.getChangeZoneTable();
        if (triggerList == null) {
            triggerList = new CardZoneTable();
            useZoneTable = false;
        }
        if (sa.hasParam("ChangeZoneTable")) {
            sa.setChangeZoneTable(triggerList);
            useZoneTable = true;
        }

        MutableBoolean combatChanged = new MutableBoolean(false);
        for (final Player owner : AbilityUtils.getDefinedPlayers(host, sa.getParamOrDefault("TokenOwner", "You"), sa)) {
            makeTokens(prototype, owner, sa, finalAmount, true, false, triggerList, combatChanged);
        }

        if (!useZoneTable) {
            triggerList.triggerChangesZoneAll(game, sa);
            triggerList.clear();
        }

        game.fireEvent(new GameEventTokenCreated());

        if (combatChanged.isTrue()) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }
    }
}
