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
package forge.game.spellability;

import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.event.GameEventLandPlayed;
import forge.game.player.Player;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

public class LandAbility extends Ability {

    public LandAbility(Card sourceCard, Player p, StaticAbility mayPlay) {
        super(sourceCard, (Cost)null);
        setActivatingPlayer(p);
        setMayPlay(mayPlay);
    }
    public LandAbility(Card sourceCard) {
        this(sourceCard, sourceCard.getController(), (StaticAbility)null);
    }
    @Override
    public boolean canPlay() {
        final Card land = this.getHostCard();
        final Player p = this.getActivatingPlayer();
        final Game game = p.getGame();
        if (!p.canCastSorcery()) {
            return false;
        }

        // CantBeCast static abilities
        for (final Card ca : game.getCardsIn(ZoneType.listValueOf("Battlefield,Command"))) {
            final Iterable<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                if (stAb.applyAbility("CantPlayLand", land, this)) {
                    return false;
                }
            }
        }

        if (land != null) {
            final boolean mayPlay = getMayPlay() != null;
            if (land.getOwner() != p && !mayPlay) {
                return false;
            }

            final Zone zone = game.getZoneOf(land);
            if (zone != null && (zone.is(ZoneType.Battlefield) || (!zone.is(ZoneType.Hand) && !mayPlay))) {
                return false;
            }
        }

        // **** Check for land play limit per turn ****
        // Dev Mode
        if (p.getController().canPlayUnlimitedLands() || p.hasKeyword("You may play any number of additional lands on each of your turns.")) {
            return true;
        }

        // check for adjusted max lands play per turn
        int adjMax = 1;
        for (String keyword : p.getKeywords()) {
            if (keyword.startsWith("AdjustLandPlays")) {
                final String[] k = keyword.split(":");
                adjMax += Integer.valueOf(k[1]);
            }
        }
        if (p.getLandsPlayedThisTurn() < adjMax) {
            return true;
        }
        return false;
    }
    @Override
    public void resolve() {
        final Card land = this.getHostCard();
        final Player p = this.getActivatingPlayer();
        final Game game = p.getGame();
        
        land.setController(p, 0);
        if (land.isFaceDown()) {
            land.turnFaceUp();
        }
        game.getAction().moveTo(p.getZone(ZoneType.Battlefield), land, null, Maps.newHashMap());

        // play a sound
        game.fireEvent(new GameEventLandPlayed(p, land));

        // Run triggers
        final Map<String, Object> runParams = Maps.newHashMap();
        runParams.put("Card", land);
        game.getTriggerHandler().runTrigger(TriggerType.LandPlayed, runParams, false);
        game.getStack().unfreezeStack();
        p.addLandPlayedThisTurn();

        // increase mayplay used
        if (getMayPlay() != null) {
            getMayPlay().incMayPlayTurn();
        }
    }
    @Override
    public String toUnsuppressedString() {
        StringBuilder sb = new StringBuilder("Play land");
        StaticAbility sta = getMayPlay();
        if (sta != null) {
            Card source = sta.getHostCard();
            if (!source.equals(getHostCard())) {
                sb.append(" by ");
                if ((source.isEmblem() || source.getType().hasSubtype("Effect"))
                        && source.getEffectSource() != null) {
                    sb.append(source.getEffectSource());
                } else {
                    sb.append(source);
                }
                if (sta.hasParam("MayPlayText")) {
                    sb.append(" (").append(sta.getParam("MayPlayText")).append(")");
                }
            }
        }
        return sb.toString();
    }
    
}