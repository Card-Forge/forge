package forge;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;

import net.slightlymagic.braids.util.generator.GeneratorFunctions;
import net.slightlymagic.braids.util.lambda.Lambda1;

import com.google.code.jyield.Generator;
import com.google.code.jyield.YieldUtils;

import forge.card.spellability.SpellAbility;
import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * NameChanger class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class NameChanger {
    private Map<String, String> mutatedMap = new TreeMap<String, String>();
    private Map<String, String> originalMap = new TreeMap<String, String>();

    private boolean changeCardName;

    /**
     * <p>
     * Constructor for NameChanger.
     * </p>
     */
    public NameChanger() {
        // readFile();
        setShouldChangeCardName(false);
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
        return changeCardName;
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
        changeCardName = b;
    }

    /**
     * This change's the inputGenerator's Card instances in place, and returns a
     * generator of those same changed instances.
     * 
     * TODO Should this method return void, because it side effects the contents
     * of its inputGenerator?
     * 
     * @param inputGenerator
     *            a Generator of Card objects
     * @return a Generator of side-effected Card objects
     */
    public final Generator<Card> changeCard(final Generator<Card> inputGenerator) {

        // Create a new Generator by applying a transform to the
        // inputGenerator.

        Lambda1<Card, Card> transform = new Lambda1<Card, Card>() {
            public Card apply(final Card toChange) {
                return changeCard(toChange);
            };
        };

        return GeneratorFunctions.transformGenerator(transform, inputGenerator);
    }

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
        String newName = changeName(c.getName());
        c.setName(newName);

        // change text
        String s;
        s = c.getSpellText();
        c.setText(changeString(c, s));

        // change all SpellAbilities
        SpellAbility[] spell = c.getSpellAbility();
        for (int i = 0; i < spell.length; i++) {
            s = spell[i].getStackDescription();
            spell[i].setStackDescription(changeString(c, s));

            s = spell[i].toString();
            spell[i].setDescription(changeString(c, s));
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
        if (shouldChangeCardName()) {
            list = new CardList(changeCard(YieldUtils.toGenerator(list)));
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
        Object o = mutatedMap.get(originalName);

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
        Object o = originalMap.get(mutatedName);

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
            BufferedReader in = new BufferedReader(new FileReader(ForgeProps.getFile(NewConstants.NAME_MUTATOR)));

            String line = in.readLine();

            // stop reading if end of file or blank line is read
            while (line != null && (line.trim().length() != 0)) {
                processLine(line.trim());

                line = in.readLine();
            } // while
        } // try
        catch (Exception ex) {
            // ~ throw new RuntimeException("NameMutator : readFile() error, "
            // +ex);

            // ~ (could be cleaner...)
            try {
                BufferedReader in = new BufferedReader(new FileReader(ForgeProps.getFile(NewConstants.NAME_MUTATOR)));

                String line;

                // stop reading if end of file or blank line is read
                while ((line = in.readLine()) != null && (line.trim().length() != 0)) {
                    processLine(line.trim());
                } // while
            } catch (Exception ex2) {
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
        StringTokenizer tok = new StringTokenizer(line, ":");

        if (tok.countTokens() != 2) {
            throw new RuntimeException("NameMutator : processLine() error, invalid line in file name-mutator.txt - "
                    + line);
        }

        String original = tok.nextToken().trim();
        String mutated = tok.nextToken().trim();

        mutatedMap.put(original, mutated);
        originalMap.put(mutated, original);
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
        for (Entry<String, String> e : map.entrySet()) {
            System.out.println(e.getKey() + " : " + e.getValue());
        }
    }
}
