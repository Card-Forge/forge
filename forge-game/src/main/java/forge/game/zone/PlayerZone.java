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
package forge.game.zone;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.card.CardStateName;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

/**
 * <p>
 * DefaultPlayerZone class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class PlayerZone extends Zone {
    private static final long serialVersionUID = -5687652485777639176L;

    // the this is not the owner of the card
    private static Predicate<Card> alienCardsActivationFilter(final Player who) {
        return c -> !c.mayPlay(who).isEmpty() || c.mayPlayerLook(who);
    }

    private final class OwnCardsActivationFilter implements Predicate<Card> {
        @Override
        public boolean apply(final Card c) {
            if (c.mayPlayerLook(c.getController())) {
                return true;
            }

            if (c.isLand() && !c.mayPlay(c.getController()).isEmpty()) {
                return true;
            }

            boolean graveyardCastable = c.hasKeyword(Keyword.FLASHBACK) ||
                    c.hasKeyword(Keyword.RETRACE) || c.hasKeyword(Keyword.JUMP_START) || c.hasKeyword(Keyword.ESCAPE) ||
                    c.hasKeyword(Keyword.DISTURB);
            boolean exileCastable = c.isForetold() || isOnAdventure(c);
            for (final SpellAbility sa : c.getSpellAbilities()) {
                final ZoneType restrictZone = sa.getRestrictions().getZone();

                // for mayPlay the restrictZone is null for reasons
                if (sa.isSpell() && c.mayPlay(sa.getMayPlay()) != null) {
                    return true;
                }

                if (PlayerZone.this.is(restrictZone)) {
                    return true;
                }

                //todo add brokkos??
                if (sa.isSpell()
                        && (graveyardCastable && PlayerZone.this.is(ZoneType.Graveyard))
                        && restrictZone.equals(ZoneType.Hand)) {
                    return true;
                }

                if (sa.isSpell()
                        && (exileCastable && PlayerZone.this.is(ZoneType.Exile))
                        && restrictZone.equals(ZoneType.Hand)) {
                    return true;
                }
            }
            return false;
        }
    }
    private boolean isOnAdventure(Card c) {
        if (!c.isAdventureCard())
            return false;
        if (c.getExiledWith() == null)
            return false;
        if (!CardStateName.Adventure.equals(c.getExiledWith().getCurrentStateName()))
            return false;
        return true;
    }

    private final Player player;

    public PlayerZone(final ZoneType zone, final Player inPlayer) {
        super(zone, inPlayer.getGame());
        player = inPlayer;
    }

    @Override
    protected void onChanged() {
        player.updateZoneForView(this);
    }

    public final Player getPlayer() {
        return player;
    }

    @Override
    public final String toString() {
        return Lang.getInstance().getPossessedObject(player.toString(), zoneType.toString());
    }

    public Iterable<Card> getCardsPlayerCanActivate(Player who) {
        Iterable<Card> cl = getCards(false);
        boolean checkingForOwner = who == player;

        if (checkingForOwner && (is(ZoneType.Battlefield) || is(ZoneType.Hand))) {
            return cl;
        }

        // Only check the top card of the library
        if (is(ZoneType.Library)) {
            cl = Iterables.limit(cl, 1);
        }

        final Predicate<Card> filterPredicate = checkingForOwner ? new OwnCardsActivationFilter() : alienCardsActivationFilter(who);
        return CardLists.filter(cl, filterPredicate);
    }
}
