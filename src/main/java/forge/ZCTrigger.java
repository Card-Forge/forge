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
package forge;

/**
 * <p>
 * ZCTrigger class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public enum ZCTrigger {

    /** The ENTERFIELD. */
    ENTERFIELD("comes into play", "any > field"), // explanation: zone before
                                                  // last trigger check ">" zone
                                                  // card currently in
    /** The LEAVEFIELD. */
    LEAVEFIELD("leaves play", "field > any"),

    /** The DESTROY. */
    DESTROY("is put into a graveyard from play", "field > grave"),

    /** The ENTERGRAVE. */
    ENTERGRAVE("is put into a graveyard from anywhere", "any > grave");

    /** The rule text. */
    private String ruleText;

    /** The trigger zones. */
    private String[] triggerZones;

    /**
     * <p>
     * Constructor for ZCTrigger.
     * </p>
     * 
     * @param text
     *            a {@link java.lang.String} object.
     * @param tofrom
     *            a {@link java.lang.String} object.
     */
    ZCTrigger(final String text, final String tofrom) {
        this.ruleText = text;
        this.triggerZones = tofrom.split(" > ");
    }

    /**
     * <p>
     * triggerOn.
     * </p>
     * 
     * @param sourceZone
     *            a {@link java.lang.String} object.
     * @param destintationZone
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean triggerOn(final String sourceZone, final String destintationZone) {
        return ((this.triggerZones[0].equals("any") || this.triggerZones[0].equals(sourceZone)) && (this.triggerZones[1]
                .equals("any") || this.triggerZones[0].equals(sourceZone)));
    }

    /**
     * <p>
     * getTrigger.
     * </p>
     * 
     * @param description
     *            a {@link java.lang.String} object.
     * @return a {@link forge.ZCTrigger} object.
     */
    public static ZCTrigger getTrigger(final String description) {
        for (final ZCTrigger t : ZCTrigger.values()) {
            if (t.ruleText.equals(description)) {
                return t;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.ruleText;
    }
}
