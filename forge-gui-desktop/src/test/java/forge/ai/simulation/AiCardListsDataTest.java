package forge.ai.simulation;

import static org.testng.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import forge.GuiDesktop;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.util.FileUtil;

/** The shipped AI card lists must only name cards that exist, use known roles and not repeat the bracket combos. */
public class AiCardListsDataTest {
    private static final String NAME_PREFIX = "Name:";

    private Set<String> cardNames;

    @BeforeClass
    public void setUp() {
        GuiBase.setInterface(new GuiDesktop());
        cardNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try (Stream<Path> paths = Files.walk(Path.of(ForgeConstants.CARD_DATA_DIR))) {
            paths.filter(p -> p.toString().endsWith(".txt")).forEach(this::readNames);
        } catch (final IOException e) {
            fail("Could not read card names from " + ForgeConstants.CARD_DATA_DIR, e);
        }
    }

    private void readNames(final Path path) {
        final List<String> faces = new ArrayList<>();
        for (final String line : FileUtil.readFile(path.toString())) {
            if (line.startsWith(NAME_PREFIX)) {
                final String n = line.substring(NAME_PREFIX.length()).trim();
                cardNames.add(n);
                faces.add(n);
            }
        }
        if (faces.size() > 1) {
            cardNames.add(String.join(" // ", faces));
        }
    }

    private static String strip(final String line) {
        final int i = line.indexOf('#');
        return (i < 0 ? line : line.substring(0, i)).trim();
    }

    @Test
    public void roleFileUsesKnownRolesAndRealCards() {
        final List<String> problems = new ArrayList<>();
        int n = 0;
        for (final String raw : FileUtil.readFile(ForgeConstants.AI_CARD_ROLES_FILE)) {
            n++;
            final String line = strip(raw);
            if (line.isEmpty()) {
                continue;
            }
            final String[] parts = line.split("\\s*\\|\\s*");
            final String role = parts[0].toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            try {
                AiCardLists.Role.valueOf(role);
            } catch (final IllegalArgumentException e) {
                problems.add("line " + n + ": unknown role '" + parts[0] + "'");
            }
            if (parts.length < 2) {
                problems.add("line " + n + ": role without cards");
            }
            for (int i = 1; i < parts.length; i++) {
                if (!cardNames.contains(parts[i].trim())) {
                    problems.add("line " + n + ": unknown card '" + parts[i].trim() + "'");
                }
            }
        }
        if (!problems.isEmpty()) {
            fail(ForgeConstants.AI_CARD_ROLES_FILE + ":\n" + String.join("\n", problems));
        }
    }

    @Test
    public void comboFileHasRealCardsAndNoBracketDuplicates() {
        final Set<String> bracketPairs = new HashSet<>();
        for (final String raw : FileUtil.readFile(ForgeConstants.COMMANDER_BRACKET_COMBOS_FILE)) {
            final String[] p = strip(raw).split("\\s*\\|\\s*");
            if (p.length >= 3) {
                bracketPairs.add(new AiCardLists.Pair(p[1], p[2]).key());
            }
        }
        final List<String> problems = new ArrayList<>();
        final Set<String> own = new HashSet<>();
        int n = 0;
        for (final String raw : FileUtil.readFile(ForgeConstants.AI_COMMANDER_COMBOS_FILE)) {
            n++;
            final String line = strip(raw);
            if (line.isEmpty()) {
                continue;
            }
            final String[] p = line.split("\\s*\\|\\s*");
            if (p.length < 3) {
                problems.add("line " + n + ": fewer than 3 columns");
                continue;
            }
            for (int i = 1; i <= 2; i++) {
                if (!cardNames.contains(p[i].trim())) {
                    problems.add("line " + n + ": unknown card '" + p[i].trim() + "'");
                }
            }
            final String key = new AiCardLists.Pair(p[1], p[2]).key();
            if (bracketPairs.contains(key)) {
                problems.add("line " + n + ": already in the bracket combos file: " + p[1] + " | " + p[2]);
            }
            if (!own.add(key)) {
                problems.add("line " + n + ": listed twice: " + p[1] + " | " + p[2]);
            }
        }
        if (!problems.isEmpty()) {
            fail(ForgeConstants.AI_COMMANDER_COMBOS_FILE + ":\n" + String.join("\n", problems));
        }
    }
}
