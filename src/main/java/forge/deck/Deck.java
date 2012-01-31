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
package forge.deck;

import java.io.Serializable;

import forge.PlayerType;
import forge.game.GameType;

/**
 * <p>
 * Deck class.
 * </p>
 * 
 * The set of MTG legal cards that become player's library when the game starts.
 * Any other data is not part of a deck and should be stored elsewhere. Current
 * fields allowed for deck metadata are Name, Title, Description and Deck Type.
 * 
 * @author Forge
 * @version $Id$
 */
public final class Deck implements Comparable<Deck>, Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -7478025567887481994L;

    // gameType is from Constant.GameType, like GameType.Regular

    private String name;
    private GameType deckType;
    private String comment = null;
    private PlayerType playerType = null;

    private boolean customPool = false;

    private final DeckSection main;
    private final DeckSection sideboard;

    // gameType is from Constant.GameType, like GameType.Regular
    /**
     * <p>
     * Constructor for Deck.
     * </p>
     */
    public Deck() {
        this.main = new DeckSection();
        this.sideboard = new DeckSection();
    }

    /**
     * <p>
     * Constructor for Deck.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     */
    public Deck(final GameType type) {
        this();
        this.setDeckType(type);
    }

    /**
     * <p>
     * getDeckType.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public GameType getDeckType() {
        return this.deckType;
    }

    // can only call this method ONCE
    /**
     * <p>
     * setDeckType.
     * </p>
     * 
     * @param deckType
     *            a {@link java.lang.String} object.
     */
    public void setDeckType(final GameType deckType) {
        if (this.getDeckType() != null) {
            throw new IllegalStateException("Deck : setDeckType() error, deck type has already been set");
        }

        this.deckType = deckType;
    }

    /**
     * <p>
     * setName.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public void setName(final String s) {
        this.name = s;
    }

    /**
     * <p>
     * getName.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p>
     * setComment.
     * </p>
     * 
     * @param comment
     *            a {@link java.lang.String} object.
     */
    public void setComment(final String comment) {
        this.comment = comment;
    }

    /**
     * <p>
     * getComment.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getComment() {
        return this.comment;
    }

    /**
     * <p>
     * isDraft.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isDraft() {
        return this.getDeckType().equals(GameType.Draft);
    }

    /**
     * <p>
     * isSealed.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isSealed() {
        return this.getDeckType().equals(GameType.Sealed);
    }

    /**
     * <p>
     * isRegular.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isRegular() {
        return this.getDeckType().equals(GameType.Constructed);
    }

    /**
     * <p>
     * hashCode.
     * </p>
     * 
     * @return a int.
     */
    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * <p>
     * compareTo.
     * </p>
     * 
     * @param d
     *            a {@link forge.deck.Deck} object.
     * @return a int.
     */
    @Override
    public int compareTo(final Deck d) {
        return this.getName().compareTo(d.getName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
        if (o instanceof Deck) {
            final Deck d = (Deck) o;
            return this.getName().equals(d.getName());
        }
        return false;
    }

    /**
     * Gets the player type.
     * 
     * @return the player type
     */
    public PlayerType getPlayerType() {
        return this.playerType;
    }

    /**
     * Sets the player type.
     * 
     * @param recommendedPlayer0
     *            the new player type
     */
    public void setPlayerType(final PlayerType recommendedPlayer0) {
        this.playerType = recommendedPlayer0;
    }

    /**
     * Checks if is custom pool.
     * 
     * @return true, if is custom pool
     */
    public boolean isCustomPool() {
        return this.customPool;
    }

    /**
     * Sets the custom pool.
     * 
     * @param cp
     *            the new custom pool
     */
    public void setCustomPool(final boolean cp) {
        this.customPool = cp;
    }

    /**
     * <p>
     * Getter for the field <code>main</code>.
     * </p>
     * 
     * @return a {@link java.util.List} object.
     */
    public DeckSection getMain() {
        return this.main;
    }

    /**
     * <p>
     * Getter for the field <code>sideboard</code>.
     * </p>
     * 
     * @return a {@link java.util.List} object.
     */
    public DeckSection getSideboard() {
        return this.sideboard;
    }
}
