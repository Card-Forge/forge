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

import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
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

    // the this is not the owner of the card
    private final class AlienCardsActivationFilter implements Predicate<Card> {
        @Override
        public boolean apply(final Card c) {
   
            if (c.hasStartOfKeyword("May be played by your opponent")
                    || c.hasKeyword("Your opponent may look at this card.")) {
                return true;
            }
            return false;
        }
    }

    private final class OwnCardsActivationFilter implements Predicate<Card> {
        @Override
        public boolean apply(final Card c) {
            if (c.hasKeyword("You may look at this card.")) {
                return true;
            }
   
            if (c.isLand() && (c.hasKeyword("May be played") || c.hasKeyword("May be played without paying its mana cost"))) {
                return true;
            }
   
            for (final SpellAbility sa : c.getSpellAbilities()) {
                final ZoneType restrictZone = sa.getRestrictions().getZone();
                if (PlayerZone.this.is(restrictZone)) {
                    return true;
                }
   
                if (sa.isSpell()
                        && (c.hasKeyword("May be played") || c.hasKeyword("May be played without paying its mana cost")
                                || (c.hasStartOfKeyword("Flashback") && PlayerZone.this.is(ZoneType.Graveyard)))
                        && restrictZone.equals(ZoneType.Hand)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Constant <code>serialVersionUID=-5687652485777639176L</code>. */
    private static final long serialVersionUID = -5687652485777639176L;


    private final Player player;



    /**
     * <p>
     * Constructor for DefaultPlayerZone.
     * </p>
     * 
     * @param zone
     *            a {@link java.lang.String} object.
     * @param inPlayer
     *            a {@link forge.game.player.Player} object.
     */
    public PlayerZone(final ZoneType zone, final Player inPlayer) {
        super(zone);
        if ( inPlayer == null )
            throw new IllegalArgumentException("Player zone must be initialized with correct player");
        this.player = inPlayer;
    }

    // ************ BEGIN - these methods fire updateObservers() *************

    @Override
    public void add(final Object o, boolean update) {
        final Card c = (Card) o;

        // Immutable cards are usually emblems and effects - we don't want to log those.
        if (!c.isImmutable()) {
            this.cardsAddedThisTurn.add(c);
            final Zone zone = c.getGame().getZoneOf(c);
            if (zone != null) {
                this.cardsAddedThisTurnSource.add(zone.getZoneType());
            } else {
                this.cardsAddedThisTurnSource.add(null);
            }
        }

        if (this.is(ZoneType.Graveyard)
                && c.hasKeyword("If CARDNAME would be put into a graveyard "
                        + "from anywhere, reveal CARDNAME and shuffle it into its owner's library instead.")) {
            final PlayerZone lib = c.getOwner().getZone(ZoneType.Library);
            lib.add(c);
            c.getOwner().shuffle();
            return;
        }

        if (c.isUnearthed() && (this.is(ZoneType.Graveyard) || this.is(ZoneType.Hand) || this.is(ZoneType.Library))) {
            final PlayerZone removed = c.getOwner().getZone(ZoneType.Exile);
            removed.add(c);
            c.setUnearthed(false);
            return;
        }

        c.setTurnInZone(c.getGame().getPhaseHandler().getTurn());
        this.cardList.add(c);
        
        if (!this.is(ZoneType.Battlefield)) {
            c.setTapped(false);
        }
    }


    /**
     * Checks if is.
     * 
     * @param zone
     *            a {@link java.lang.String} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a boolean
     */
    public final boolean is(final ZoneType zone, final Player player) {
        return (zone == this.zoneType && this.player.equals(player));
    }

    /**
     * <p>
     * Getter for the field <code>player</code>.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    public final Player getPlayer() {
        return this.player;
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String toString() {
        return  String.format("%s %s", Lang.getPossesive(this.player.toString()), this.zoneType);
    }

    public List<Card> getCardsPlayerCanActivate(Player who) {
    
        Iterable<Card> cl = roCardList; // copy to new AL won't help here
    
        // Only check the top card of the library
        if (is(ZoneType.Library)) {
            cl = Iterables.limit(cl, 1);
        }
        
        boolean checkingForOwner = who == this.player;

        if (checkingForOwner && (this.is(ZoneType.Battlefield) || this.is(ZoneType.Hand)))
            return roCardList;

        final Predicate<Card> filterPredicate = checkingForOwner ? new OwnCardsActivationFilter() : new AlienCardsActivationFilter();
        return Lists.newArrayList(cl = Iterables.filter(cl, filterPredicate));
    }


}
