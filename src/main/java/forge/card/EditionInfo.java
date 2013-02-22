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
package forge.card;

/**
 * <p>
 * SetInfo class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class EditionInfo {

    /** The Code. */
    private String code;

    /** The Rarity. */
    private String rarity;

    /** The URL. */
    private String url;

    /** The Pic count. */
    private int picCount;

    /**
     * <p>
     * Constructor for SetInfo.
     * </p>
     */
    public EditionInfo() {
        this.setCode("");
        this.setRarity("");
        this.setUrl("");
        this.setPicCount(0);
    }

    /**
     * <p>
     * Constructor for SetInfo.
     * </p>
     * 
     * @param c
     *            a {@link java.lang.String} object.
     * @param r
     *            a {@link java.lang.String} object.
     * @param u
     *            a {@link java.lang.String} object.
     */
    public EditionInfo(final String c, final String r, final String u) {
        this.setCode(c);
        this.setRarity(r);
        this.setUrl(u);
        this.setPicCount(0);
    }

    /**
     * <p>
     * Constructor for SetInfo.
     * </p>
     * 
     * @param c
     *            a {@link java.lang.String} object.
     * @param r
     *            a {@link java.lang.String} object.
     * @param u
     *            a {@link java.lang.String} object.
     * @param p
     *            a int.
     */
    public EditionInfo(final String c, final String r, final String u, final int p) {
        this.setCode(c);
        this.setRarity(r);
        this.setUrl(u);
        this.setPicCount(p);
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
        return this.getCode();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object o) {
        if (o instanceof EditionInfo) {
            final EditionInfo siO = (EditionInfo) o;
            return this.getCode().equals(siO.getCode());
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return (41 * (41 + this.getCode().hashCode()));
    }

    /**
     * Gets the code.
     * 
     * @return the code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Sets the code.
     * 
     * @param code0
     *            the code to set
     */
    public void setCode(final String code0) {
        this.code = code0;
    }

    /**
     * Gets the rarity.
     * 
     * @return the rarity
     */
    public String getRarity() {
        return this.rarity;
    }

    /**
     * Sets the rarity.
     * 
     * @param rarity0
     *            the rarity to set
     */
    public void setRarity(final String rarity0) {
        this.rarity = rarity0;
    }

    /**
     * Gets the url.
     * 
     * @return the url
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * Sets the url.
     * 
     * @param url0
     *            the url to set
     */
    public void setUrl(final String url0) {
        this.url = url0;
    }

    /**
     * Gets the pic count.
     * 
     * @return the picCount
     */
    public int getPicCount() {
        return this.picCount;
    }

    /**
     * Sets the pic count.
     * 
     * @param picCount0
     *            the picCount to set
     */
    public void setPicCount(final int picCount0) {
        this.picCount = picCount0;
    }
}
