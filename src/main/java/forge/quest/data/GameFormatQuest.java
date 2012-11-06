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
package forge.quest.data;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import com.google.common.base.Predicate;

import forge.Singletons;
import forge.card.CardEdition;
import forge.game.GameFormat;


/**
 * This is an alternate game format type, the main difference is that this
 * is not immutable. This class is necessary because we may wish to update
 * its contents in certain circumstances, and it was safer to create a new
 * class than to make the preset game formats modifiable.
 */
public final class GameFormatQuest extends GameFormat {

    /**
     * Instantiates a new game format based on two lists.
     * 
     * @param newName
     *      String, the name
     * @param setsToAllow
     *      List<String>, these are the allowed sets
     * @param cardsToBan
     *      List<String>, these will be the banned cards
     */
    public GameFormatQuest(final String newName, final List<String> setsToAllow, final List<String> cardsToBan) {
        super(newName, setsToAllow, cardsToBan);
    }

    /**
     * Instantiates a new game format based on an existing format.
     * 
     * @param toCopy
     *      an existing format
     */
    public GameFormatQuest(final GameFormat toCopy) {
        this(toCopy.getName(), toCopy.getAllowedSetCodes(), toCopy.getBannedCardNames());
    }


    /**
     * 
     * Updates the filters based on the current list data.
     */
    public void updateFilters() {
        // nothing to do here. 
        // predicates hold references to lists and thus get auto updated.
        
        // remove this method after reading.
    }

    /**
     * Empty the whole list.
     */
    public void emptyAllowedSets() {
        if (allowedSetCodes.isEmpty()) {
            return;
        }
        allowedSetCodes.clear();
    }

    /** 
     * @param setCode
     *      the set code to add
     */
    public void addAllowedSet(final String setCode) {
        if (!allowedSetCodes.contains(setCode)) {
            allowedSetCodes.add(setCode);
        }
    }


    /**
     * Get the list of excluded sets.
     * 
     * @return unmodifiable list of excluded sets.
     */
    public List<String> getExcludedSetCodes() {
        if (this.allowedSetCodes.isEmpty()) {
            return null;
        }

        List<String> exSets = new ArrayList<String>();

        for (CardEdition ce : Singletons.getModel().getEditions()) {
            if (!isSetLegal(ce.getCode())) {
                exSets.add(ce.getCode());
            }
        }
        return Collections.unmodifiableList(exSets);
    }

    /**
     * Add a set to allowed set codes.
     * 
     * @param setCode String, set code.
     */
    public void unlockSet(final String setCode) {
        if (this.allowedSetCodes.isEmpty()) {
            return; // We are already allowing all sets!
        } else if (this.allowedSetCodes.contains(setCode)) {
            return; // Already on the list
        }
        this.allowedSetCodes.add(setCode);
        updateFilters();
    }

    /**
     * Checks if the current format contains sets with snow-land (horrible hack...).
     * @return boolean, contains snow-land sets.
     * 
     */
    public boolean hasSnowLands() {
        return (this.isSetLegal("ICE") || this.isSetLegal("CSP"));
    }

    /**
     * The Class Predicates.
     */
    public abstract static class Predicates {
        /**
         * Checks if is legal in quest format.
         *
         * @param qFormat the format
         * @return the predicate
         */
        public static Predicate<CardEdition> isLegalInFormatQuest(final GameFormatQuest qFormat) {
            return new LegalInFormatQuest(qFormat);
        }

      private static class LegalInFormatQuest implements Predicate<CardEdition> {
        private final GameFormatQuest qFormat;

        public LegalInFormatQuest(final GameFormatQuest fmt) {
            this.qFormat = fmt;
        }

        @Override
        public boolean apply(final CardEdition subject) {
            return this.qFormat.isSetLegal(subject.getCode());
        }
      }
    }


}
