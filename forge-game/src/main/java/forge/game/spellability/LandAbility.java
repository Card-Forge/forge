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

import org.apache.commons.lang3.ObjectUtils;

import forge.card.CardStateName;
import forge.card.mana.ManaCost;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class LandAbility extends AbilityStatic {

    public LandAbility(Card sourceCard) {
        super(sourceCard, ManaCost.NO_COST);

        getRestrictions().setZone(ZoneType.Hand);
    }

    public boolean canPlay(Card newHost) {
        final Player p = getActivatingPlayer();
        return p.canPlayLand(newHost, false, this);
    }

    @Override
    public boolean isLandAbility() { return true; }

    @Override
    public boolean isSecondary() {
        return true;
    }

    @Override
    public boolean canPlay() {
        Card land = this.getHostCard();
        final Player p = this.getActivatingPlayer();
        if (p == null) {
            return false;
        }

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
        final Card result = getActivatingPlayer().playLandNoCheck(getHostCard());

        // increase mayplay used
        this.incMayPlayedThisTurn();

        // if land isn't in battlefield try to reset the card state
        if (result != null && !result.isInZone(ZoneType.Battlefield)) {
            result.setState(CardStateName.Original, true);
        }
    }
    @Override
    public String toUnsuppressedString() {
        StringBuilder sb = new StringBuilder("Play land");

        if (getHostCard().isModal()) {
            sb.append(" (").append(getHostCard().getName(ObjectUtils.firstNonNull(getCardStateName(), CardStateName.Original))).append(")");
        }

        return sb.toString();
    }

    @Override
    public Card getAlternateHost(Card source) {
        boolean lkicheck = false;

        // need to be done before so it works with Vivien and Zoetic Cavern
        if (source.isFaceDown() && source.isInZone(ZoneType.Exile)) {
            if (!source.isLKI()) {
                source = CardUtil.getLKICopy(source);
            }

            source.forceTurnFaceUp();
            lkicheck = true;
        }

        if (getCardState() != null && source.getCurrentStateName() != getCardStateName()) {
            if (!source.isLKI()) {
                source = CardUtil.getLKICopy(source);
            }
            CardStateName stateName = getCardState().getStateName();
            if (!source.hasState(stateName)) {
                source.addAlternateState(stateName, false);
                source.getState(stateName).copyFrom(getHostCard().getState(stateName), true);
            }

            source.setState(stateName, false);
            if (getHostCard().hasBackSide()) {
                source.setBackSide(getHostCard().getRules().getSplitType().getChangedStateName().equals(stateName));
            }

            // need to reset CMC
            source.setLKICMC(-1);
            source.setLKICMC(source.getCMC());
            lkicheck = true;
        }

        return lkicheck ? source : null;
    }
}