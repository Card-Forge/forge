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


import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardEdition;
import forge.card.CardRulesPredicates;
import forge.game.GameFormat;
import forge.item.CardPrinted;


/**
 * This is an alternate game format type, the main difference is that this
 * is not immutable. This class is necessary because we may wish to update
 * its contents in certain circumstances, and it was safer to create a new
 * class than to make the preset game formats modifiable.
 */
public final class GameFormatQuest {

    private final String name;
    // contains allowed sets, when empty allows all sets
    private List<String> allowedSetCodes;
    private List<String> bannedCardNames;

    private Predicate<CardPrinted> filterRules;
    private Predicate<CardPrinted> filterPrinted;

    /**
     * Instantiates a new game format based on an existing format.
     * 
     * @param toCopy
     *      an existing format
     */
    public GameFormatQuest(final GameFormat toCopy) {
        this.name = new String(toCopy.getName());

        this.allowedSetCodes = new ArrayList<String>();
        allowedSetCodes.addAll(toCopy.getAllowedSetCodes());

        this.bannedCardNames = new ArrayList<String>();
        bannedCardNames.addAll(toCopy.getBannedCardNames());

        this.filterRules = this.buildFilterRules();
        this.filterPrinted = this.buildFilterPrinted();
    }

    /**
     * Instantiates an empty new game format.
     * 
     * @param newName
     *      String, the name
     */
    public GameFormatQuest(final String newName) {
        this.name = new String(newName);

        this.allowedSetCodes = new ArrayList<String>();
        this.bannedCardNames = new ArrayList<String>();
    }


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
        this.name = new String(newName);

        this.allowedSetCodes = new ArrayList<String>();
        allowedSetCodes.addAll(setsToAllow);

        this.bannedCardNames = new ArrayList<String>();
        bannedCardNames.addAll(cardsToBan);

        this.filterRules = this.buildFilterRules();
        this.filterPrinted = this.buildFilterPrinted();
    }

    private Predicate<CardPrinted> buildFilterPrinted() {
        final Predicate<CardPrinted> banNames = CardPrinted.Predicates.namesExcept(this.bannedCardNames);
        if (this.allowedSetCodes == null || this.allowedSetCodes.isEmpty()) {
            return banNames;
        }
        return com.google.common.base.Predicates.and(banNames, CardPrinted.Predicates.printedInSets(this.allowedSetCodes, true));
    }

    private Predicate<CardPrinted> buildFilterRules() {
        final Predicate<CardPrinted> banNames = CardPrinted.Predicates.namesExcept(this.bannedCardNames);
        if (this.allowedSetCodes == null || this.allowedSetCodes.isEmpty()) {
            return banNames;
        }
        return com.google.common.base.Predicates.and(banNames, com.google.common.base.Predicates.compose(CardRulesPredicates.wasPrintedInSets(this.allowedSetCodes), CardPrinted.FN_GET_RULES));
    }

    /**
     * 
     * Updates the filters based on the current list data.
     */
    public void updateFilters() {
        this.filterRules = this.buildFilterRules();
        this.filterPrinted = this.buildFilterPrinted();
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
     * @param banCard
     *      the card to ban
     */
    public void addBannedCard(final String banCard) {
        if (!bannedCardNames.contains(banCard)) {
            bannedCardNames.add(banCard);
        }
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the filter rules.
     * 
     * @return the filter rules
     */
    public Predicate<CardPrinted> getFilterRules() {
        return this.filterRules;
    }

    /**
     * Gets the filter printed.
     * 
     * @return the filter printed
     */
    public Predicate<CardPrinted> getFilterPrinted() {
        return this.filterPrinted;
    }

    /**
     * Gets the set list.
     * 
     * @return list of allowed set codes
     */
    public List<String> getAllowedSetCodes() {
        return Collections.unmodifiableList(this.allowedSetCodes);
    }

    /**
     * Gets the banned cards.
     * 
     * @return list of banned card names
     */
    public List<String> getBannedCardNames() {
        return Collections.unmodifiableList(this.bannedCardNames);
    }

    /**
     * Checks if is sets the legal.
     * 
     * @param setCode
     *            the set code
     * @return true, if is sets the legal
     */
    public boolean isSetLegal(final String setCode) {
        return this.allowedSetCodes.isEmpty() || this.allowedSetCodes.contains(setCode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.name + " (format)";
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

    public static final Function<GameFormat, String> FN_GET_NAME = new Function<GameFormat, String>() {
        @Override
        public String apply(GameFormat arg1) {
            return arg1.getName();
        }
    };

}
