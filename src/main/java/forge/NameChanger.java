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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;

import com.google.common.collect.Lists;

import forge.card.spellability.SpellAbility;
import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.util.closures.Lambda1;

/**
 * <p>
 * NameChanger class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class NameChanger {
    private final Map<String, String> mutatedMap = new TreeMap<String, String>();
    private final Map<String, String> originalMap = new TreeMap<String, String>();

    private boolean changeCardName;

    /**
     * <p>
     * Constructor for NameChanger.
     * </p>
     */
    public NameChanger() {
        // readFile();
        this.setShouldChangeCardName(false);
    }

    // should change card name?
    /**
     * <p>
     * shouldChangeCardName.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean shouldChangeCardName() {
        return this.changeCardName;
    }

    /**
     * <p>
     * setShouldChangeCardName.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setShouldChangeCardName(final boolean b) {
        this.changeCardName = b;
    }

    public final Lambda1<Card, Card> fnTransformCard = new Lambda1<Card, Card>() {
        @Override
        public Card apply(final Card toChange) {
            return NameChanger.this.changeCard(toChange);
        };
    };    
    
    // changes card name, getText(), and all SpellAbility getStackDescription()
    // and toString()
    /**
     * <p>
     * changeCard.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card changeCard(final Card c) {
        // change name
        final String newName = this.changeName(c.getName());
        c.setName(newName);

        // change text
        String s;
        s = c.getSpellText();
        c.setText(this.changeString(c, s));

        // change all SpellAbilities
        final SpellAbility[] spell = c.getSpellAbility();
        for (final SpellAbility element : spell) {
            s = element.getStackDescription();
            element.setStackDescription(this.changeString(c, s));

            s = element.toString();
            element.setDescription(this.changeString(c, s));
        }

        return c;
    } // getMutatedCard()

    /**
     * <p>
     * changeString.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param in
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public final String changeString(final Card c, final String in) {
        // String name = getOriginalName(c.getName()); // unused
        // in = in.replaceAll(name, changeName(name));

        return in;
    }

    /**
     * Changes a list of cards if shouldChangeCardName() is true.
     * 
     * If not, we just return list.
     * 
     * TODO Should this method return void, because it side effects the contents
     * of its input list?
     * 
     * @param list
     *            the list of cards to possibly change; while this list is not
     *            affected, its contents might be
     * 
     * @return either list itself or a new list (possibly wasteful) containing
     *         the side effected cards
     */
    public final CardList changeCardsIfNeeded(CardList list) {
        if (this.shouldChangeCardName()) {
            list = new CardList( Lists.transform(list, fnTransformCard) );
        }
        return list;
    }

    // always returns mutated (alias) for the card name
    // if argument is a mutated name, it returns the same mutated name
    /**
     * <p>
     * changeName.
     * </p>
     * 
     * @param originalName
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public final String changeName(final String originalName) {
        final Object o = this.mutatedMap.get(originalName);

        if (o == null) {
            return originalName;
        }

        return o.toString();
    } // getMutatedName()

    // always returns the original cardname
    // if argument is a original name, it returns the same original name
    /**
     * <p>
     * getOriginalName.
     * </p>
     * 
     * @param mutatedName
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public final String getOriginalName(final String mutatedName) {
        final Object o = this.originalMap.get(mutatedName);

        if (o == null) {
            return mutatedName;
        }

        return o.toString();
    } // getOriginalName()

    /**
     * <p>
     * readFile.
     * </p>
     */
    @SuppressWarnings("unused")
    private void readFile() {
        try {
            final BufferedReader in = new BufferedReader(new FileReader(ForgeProps.getFile(NewConstants.NAME_MUTATOR)));

            String line = in.readLine();

            // stop reading if end of file or blank line is read
            while ((line != null) && (line.trim().length() != 0)) {
                this.processLine(line.trim());

                line = in.readLine();
            } // while
        } // try
        catch (final Exception ex) {
            // ~ throw new RuntimeException("NameMutator : readFile() error, "
            // +ex);

            // ~ (could be cleaner...)
            try {
                final BufferedReader in = new BufferedReader(new FileReader(
                        ForgeProps.getFile(NewConstants.NAME_MUTATOR)));

                String line;

                // stop reading if end of file or blank line is read
                while ((line = in.readLine()) != null) {
                    if (line.trim().length() != 0) {
                        this.processLine(line.trim());
                    } else {
                        break;
                    }
                } // while
            } catch (final Exception ex2) {
                // Show orig exception
                ErrorViewer.showError(ex2);
                throw new RuntimeException(String.format("NameMutator : readFile() error, %s", ex), ex);
            }
            // ~
        }
    } // readFile()

    // line is formated "original card name : alias card name"
    /**
     * <p>
     * processLine.
     * </p>
     * 
     * @param line
     *            a {@link java.lang.String} object.
     */
    private void processLine(final String line) {
        final StringTokenizer tok = new StringTokenizer(line, ":");

        if (tok.countTokens() != 2) {
            throw new RuntimeException("NameMutator : processLine() error, invalid line in file name-mutator.txt - "
                    + line);
        }

        final String original = tok.nextToken().trim();
        final String mutated = tok.nextToken().trim();

        this.mutatedMap.put(original, mutated);
        this.originalMap.put(mutated, original);
    }

    /**
     * <p>
     * printMap.
     * </p>
     * 
     * @param map
     *            a {@link java.util.Map} object.
     */
    @SuppressWarnings("unused")
    // printMap
    private void printMap(final Map<String, String> map) {
        for (final Entry<String, String> e : map.entrySet()) {
            System.out.println(e.getKey() + " : " + e.getValue());
        }
    }
}
