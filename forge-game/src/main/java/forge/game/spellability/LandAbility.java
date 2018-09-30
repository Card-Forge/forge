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

import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.staticability.StaticAbility;

public class LandAbility extends Ability {

    public LandAbility(Card sourceCard, Player p, StaticAbility mayPlay) {
        super(sourceCard, (Cost)null);
        setActivatingPlayer(p);
        setMayPlay(mayPlay);
    }
    public LandAbility(Card sourceCard) {
        this(sourceCard, sourceCard.getController(), null);
    }
    @Override
    public boolean canPlay() {
        final Card land = this.getHostCard();
        final Player p = this.getActivatingPlayer();

        return p.canPlayLand(land, false, this);
    }
    @Override
    public void resolve() {
        getActivatingPlayer().playLandNoCheck(getHostCard());

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