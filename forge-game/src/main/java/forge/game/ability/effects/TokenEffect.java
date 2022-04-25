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

import java.util.Arrays;
import java.util.List;

import forge.util.Lang;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class TokenEffect extends TokenEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("SpellDescription")) {
            String desc = sa.getParam("SpellDescription");
            if (StringUtils.containsIgnoreCase(desc,"Create")) {
                final Card host = sa.getHostCard();
                final List<Player> creators = AbilityUtils.getDefinedPlayers(host, sa.getParamOrDefault("TokenOwner",
                        "You"), sa);
                String verb = creators.size() == 1 ? "creates" : "create";
                String start = Lang.joinHomogenous(creators) + " " + verb;
                String create = desc.contains("Create") ? "Create" : "create";
                desc = desc.replaceFirst(".*" + create, "");
                desc = start + desc;
                //try to put the right amount of tokens for X calculations and the like
                if (sa.hasParam("TokenAmount") && !StringUtils.isNumeric(sa.getParam("TokenAmount"))) {
                    final int numTokens = AbilityUtils.calculateAmount(host, sa.getParam("TokenAmount"), sa);
                    if (numTokens != 0) { //0 probably means calculation isn't ready in time for stack
                        if (numTokens != 1) { //if we are making more than one, substitute the numeral for a/an
                            String numeral = " " + Lang.getNumeral(numTokens) + " ";
                            List<String> words = Arrays.asList(desc.split(" "));
                            String target = " " + words.get(words.indexOf(verb) + 1) + " ";
                            desc = desc.replaceFirst(target, numeral);
                        }
                        //try to cut out unneeded description, which would now be confusing
                        String truncate = null;
                        if (desc.contains(", where")) {
                            truncate = ", where";
                        } else if (desc.contains(" for each")) {
                            truncate = " for each";
                        }
                        if (truncate != null) { //if we do truncate, make sure the string ends properly
                            desc = desc.split(truncate)[0];
                            if (desc.endsWith("token") && numTokens > 1) {
                                desc = desc + "s.";
                            } else {
                                desc = desc + ".";
                            }
                        }
                    }
                }
                //pronoun replacement for things that create an amount based on what you control
                desc = desc.replace("you control","they control");
            }
            return desc;
        }
        return sa.getDescription();
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

        final int finalAmount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("TokenAmount", "1"), sa);
        MutableBoolean combatChanged = new MutableBoolean(false);

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

        makeTokenTable(AbilityUtils.getDefinedPlayers(host, sa.getParamOrDefault("TokenOwner", "You"), sa),
                sa.getParam("TokenScript").split(","), finalAmount, false, triggerList, combatChanged, sa);

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
