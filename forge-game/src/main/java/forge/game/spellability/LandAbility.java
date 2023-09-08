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

import forge.card.CardStateName;
import forge.card.mana.ManaCost;
import forge.game.card.Card;
import forge.game.card.CardPlayOption;
import forge.game.card.CardUtil;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.staticability.StaticAbility;
import forge.util.CardTranslation;
import forge.util.Localizer;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class LandAbility extends Ability {

    public LandAbility(Card sourceCard, Player p, CardPlayOption mayPlay) {
        super(sourceCard, new Cost(ManaCost.NO_COST, false));
        setActivatingPlayer(p);
        setMayPlay(mayPlay);
    }
    public LandAbility(Card sourceCard) {
        this(sourceCard, sourceCard.getController(), null);
    }

    public boolean canPlay(Card newHost) {
        final Player p = getActivatingPlayer();
        return p.canPlayLand(newHost, false, this);
    }

    @Override
    public boolean canPlay() {
        Card land = this.getHostCard();
        final Player p = this.getActivatingPlayer();

        if (this.getCardState() != null && land.getCurrentStateName() != this.getCardStateName()) {
            if (!land.isLKI()) {
                land = CardUtil.getLKICopy(land);
            }
            CardStateName stateName = getCardStateName();
            if (!land.hasState(stateName)) {
                land.addAlternateState(stateName, false);
                land.getState(stateName).copyFrom(getHostCard().getState(stateName), true);
            }

            land.setState(stateName, false);

            // need to reset CMC
            land.setLKICMC(-1);
            land.setLKICMC(land.getCMC());
        }

        return p.canPlayLand(land, false, this);
    }

    @Override
    public void resolve() {
        getHostCard().setSplitStateToPlayAbility(this);
        final Card result = getActivatingPlayer().playLandNoCheck(getHostCard(), this);

        // increase mayplay used
        if (getMayPlay() != null) {
            getMayPlay().incMayPlayTurn();
        }
        // if land isn't in battlefield try to reset the card state
        if (result != null && !result.isInPlay()) {
            result.setState(CardStateName.Original, true);
        }
    }

    @Override
    public String toUnsuppressedString() {

        Localizer localizer = Localizer.getInstance();
        StringBuilder sb = new StringBuilder(StringUtils.capitalize(localizer.getMessage("lblPlayLand")));

        if (getHostCard().isModal()) {
            sb.append(" (").append(CardTranslation.getTranslatedName(getHostCard().getName(ObjectUtils.firstNonNull(getCardStateName(), CardStateName.Original)))).append(")");
        }

        StaticAbility sta = getMayPlay();
        if (sta != null) {
            Card source = sta.getHostCard();
            if (!source.equals(getHostCard())) {
                sb.append(" by ");
                if (source.isImmutable() && source.getEffectSource() != null) {
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