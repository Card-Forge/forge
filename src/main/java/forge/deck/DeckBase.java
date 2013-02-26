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

/**
 * TODO: Write javadoc for this type.
 * 
 */
public abstract class DeckBase implements Serializable, Comparable<DeckBase> {
    private static final long serialVersionUID = -7538150536939660052L;
    // gameType is from Constant.GameType, like GameType.Regular

    private final String name;
    private String comment = null;

    /**
     * Instantiates a new deck base.
     *
     * @param name0 the name0
     */
    public DeckBase(final String name0) {
        this.name = name0;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final DeckBase d) {
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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (this.name.hashCode() * 17) + this.name.hashCode();
    }

    /* (non-Javadoc)
     * @see forge.util.IHasName#getName()
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the comment.
     *
     * @param comment the new comment
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
     * New instance.
     *
     * @param name0 the name0
     * @return the deck base
     */
    protected abstract DeckBase newInstance(String name0);

    /**
     * Clone fields to.
     *
     * @param clone the clone
     */
    protected void cloneFieldsTo(final DeckBase clone) {
        clone.comment = this.comment;
    }

    /**
     * Copy to.
     *
     * @param name0 the name0
     * @return the deck base
     */
    public DeckBase copyTo(final String name0) {
        final DeckBase obj = this.newInstance(name0);
        this.cloneFieldsTo(obj);
        return obj;
    }

    /**
     * Gets the best file name.
     *
     * @return the best file name
     */
    public final String getBestFileName() {
        return this.getName().replaceAll("[^-_$#@.,{[()]} a-zA-Z0-9]", "");
    }

}
