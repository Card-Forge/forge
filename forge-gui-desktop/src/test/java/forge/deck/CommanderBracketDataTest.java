package forge.deck;

import forge.GuiDesktop;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.util.CardScriptNames;
import forge.util.FileUtil;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.testng.Assert.fail;

public class CommanderBracketDataTest {
    @BeforeClass
    public void setUp() {
        GuiBase.setInterface(new GuiDesktop());
    }

    @Test
    public void bracketCardNamesResolveInDatabase() {
        final Set<String> cardNames = CardScriptNames.namesWithFlavor();
        final Map<String, List<String>> unknownCards = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (final String filename : List.of(
                ForgeConstants.COMMANDER_BRACKET_GAMECHANGERS_FILE,
                ForgeConstants.COMMANDER_BRACKET_MASS_LAND_DENIAL_FILE,
                ForgeConstants.COMMANDER_BRACKET_EXTRA_TURNS_FILE,
                ForgeConstants.COMMANDER_BRACKET_CHAINED_EXTRA_TURNS_FILE)) {
            validateFile(cardNames, filename, false, unknownCards);
        }
        validateFile(cardNames, ForgeConstants.COMMANDER_BRACKET_COMBOS_FILE, true, unknownCards);

        if (!unknownCards.isEmpty()) {
            fail("Commander bracket list contains " + unknownCards.size()
                    + " unknown card name(s):\n" + formatUnknownCards(unknownCards));
        }
    }

    private static void validateFile(final Set<String> cardNames, final String filename, final boolean comboFile,
                                     final Map<String, List<String>> unknownCards) {
        int lineNumber = 0;
        for (final String line : FileUtil.readFile(filename)) {
            lineNumber++;
            final String data = stripComment(line).trim();
            if (data.isEmpty()) {
                continue;
            }

            if (!comboFile) {
                validateCardName(cardNames, filename, lineNumber, data, unknownCards);
                continue;
            }

            final String[] parts = data.split("\\s*\\|\\s*");
            if (parts.length < 3) {
                addUnknownCard(unknownCards, data, filename, lineNumber, "fewer than 3 columns");
                continue;
            }
            validateCardName(cardNames, filename, lineNumber, parts[1].trim(), unknownCards);
            validateCardName(cardNames, filename, lineNumber, parts[2].trim(), unknownCards);
        }
    }

    private static void validateCardName(final Set<String> cardNames, final String filename, final int lineNumber,
                                         final String cardName, final Map<String, List<String>> unknownCards) {
        if (!cardNames.contains(cardName)) {
            addUnknownCard(unknownCards, cardName, filename, lineNumber, null);
        }
    }

    private static void addUnknownCard(final Map<String, List<String>> unknownCards, final String cardName,
                                       final String filename, final int lineNumber, final String note) {
        String location = filename + ":" + lineNumber;
        if (note != null) {
            location += " (" + note + ")";
        }
        unknownCards.computeIfAbsent(cardName, key -> new ArrayList<>()).add(location);
    }

    private static String formatUnknownCards(final Map<String, List<String>> unknownCards) {
        final StringBuilder errors = new StringBuilder();
        for (final Map.Entry<String, List<String>> entry : unknownCards.entrySet()) {
            errors.append(entry.getKey())
                    .append(" at ")
                    .append(String.join(", ", entry.getValue()))
                    .append("\n");
        }
        return errors.toString();
    }

    private static String stripComment(final String line) {
        final int commentIndex = line.indexOf('#');
        return commentIndex < 0 ? line : line.substring(0, commentIndex);
    }
}
