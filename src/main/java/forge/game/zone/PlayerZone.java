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

import forge.Card;

import forge.Singletons;
import forge.game.player.Player;

/**
 * <p>
 * DefaultPlayerZone class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class PlayerZone extends Zone {
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
        this.player = inPlayer;

    }

    // ************ BEGIN - these methods fire updateObservers() *************

    @Override
    public void add(final Object o, boolean update) {
        final Card c = (Card) o;

        // Immutable cards are usually emblems,effects and the mana pool and we
        // don't want to log those.
        if (!c.isImmutable()) {
            this.cardsAddedThisTurn.add(c);
            final Zone zone = Singletons.getModel().getGame().getZoneOf(c);
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

        c.addObserver(this);
        c.setTurnInZone(Singletons.getModel().getGame().getPhaseHandler().getTurn());

        if (!this.is(ZoneType.Battlefield)) {
            c.setTapped(false);
        }

        this.cardList.add(c);
        
        if (update) {
            this.update();
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
        return (zone == this.zoneName && this.player.equals(player));
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
        return this.player != null ? String.format("%s %s", this.player, this.zoneName) : this.zoneName.toString();
    }


    /**
     * TODO: Write javadoc for this method.
     */
    @Override
    public void updateLabelObservers() {
        getPlayer().updateLabelObservers();
    }
    

}
