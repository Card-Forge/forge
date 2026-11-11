package forge.ai.simulation;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import forge.GuiDesktop;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.util.FileUtil;

/**
 * The shipped combo list (ai-combos.txt): every line must be a combo the catalog can read, name cards that exist, and not
 * repeat another line. Add a combo to the file and this test checks it; the failure names the line.
 */
public class AiCombosDataTest {
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
            if (line.startsWith("Name:")) {
                final String n = line.substring("Name:".length()).trim();
                cardNames.add(n);
                faces.add(n);
            }
        }
        if (faces.size() > 1) {
            cardNames.add(String.join(" // ", faces));
        }
    }

    @Test
    public void everyLineIsAComboWithRealCardsAndNoRepeat() {
        final List<String> lines = FileUtil.readFile(ForgeConstants.AI_COMBOS_FILE);
        assertTrue(!lines.isEmpty(), "the combo file " + ForgeConstants.AI_COMBOS_FILE + " is missing or empty");
        final List<String> problems = new ArrayList<>();
        final List<ComboCatalog.Entry> entries = ComboCatalog.parse(lines, problems);
        final Set<String> names = new HashSet<>();
        final Set<String> keys = new HashSet<>();
        for (final ComboCatalog.Entry e : entries) {
            if (!names.add(e.name.toLowerCase())) {
                problems.add("the combo '" + e.name + "' is in the file twice");
            }
            final List<String> sorted = new ArrayList<>(e.slotSpecs());
            java.util.Collections.sort(sorted);
            if (!keys.add(String.join("|", sorted).toLowerCase())) {
                problems.add("the combo '" + e.name + "' has the same cards as another one");
            }
            for (final String slot : e.slotSpecs()) {
                for (final String card : slot.split(" / ")) {
                    if (!cardNames.contains(card.trim())) {
                        problems.add("'" + e.name + "': no card named '" + card.trim() + "'");
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), "ai-combos.txt: " + String.join("; ", problems));
    }
}
