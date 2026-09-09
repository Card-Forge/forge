package forge.game.ability;

import forge.card.CardRules;
import forge.card.ICardFace;
import forge.util.Lang;

import org.testng.AssertJUnit;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Checks that every {@code SubAbility$} and related reference in the card corpus names an SVar the
 * same face defines.
 *
 * <p>An unresolved reference is invisible at runtime: {@link AbilityFactory#getSubAbility} prints
 * "SubAbility 'X' not found" to stdout and returns null, so the chain ends early and the card
 * quietly does less than its text says. Nothing fails at load, which is how such a script gets
 * committed and shipped.
 */
public class CardScriptSubAbilityTest {

    /** Params whose value names an SVar holding an ability. */
    private static final Set<String> SUB_ABILITY_KEYS = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    static {
        SUB_ABILITY_KEYS.addAll(AbilityFactory.additionalAbilityKeys);
        SUB_ABILITY_KEYS.add("SubAbility");
        SUB_ABILITY_KEYS.add("PreventionSubAbility");
    }

    /** APIs whose {@code Choices$} is a list of SVar names rather than a valid string. */
    private static final Set<String> CHOICE_APIS =
            Set.of("Charm", "GenericChoice", "AssignGroup", "VillainousChoice", "Vote");

    /** The keys that can lead a param map and name an API. */
    private static final List<String> API_KEYS = List.of("SP", "AB", "DB", "ST", "RE");

    @Test
    public void everySubAbilityReferenceResolves() throws IOException {
        Path cardsfolder = findCardsfolder();

        // CardFace.assignMissingFieldsToVariant reaches for the localisation singleton when a
        // functional variant has a flavour name.
        Lang.createInstance("en-US");

        List<String> unresolved = new ArrayList<>();
        int cards = 0;
        CardRules.Reader reader = new CardRules.Reader();
        for (Path script : scripts(cardsfolder)) {
            String filename = script.getFileName().toString().replaceFirst("\\.txt$", "");
            CardRules rules = reader.readCard(Files.readAllLines(script, StandardCharsets.UTF_8), filename);
            cards++;
            for (ICardFace face : rules.getAllFaces()) {
                checkFace(filename, face, unresolved);
                if (face.hasFunctionalVariants()) {
                    for (ICardFace variant : face.getFunctionalVariants().values()) {
                        checkFace(filename, variant, unresolved);
                    }
                }
            }
        }

        AssertJUnit.assertTrue("read " + cards + " card scripts, expected the whole corpus", cards > 30000);
        AssertJUnit.assertTrue(
                "card scripts name sub-abilities that do not exist:\n" + String.join("\n", unresolved),
                unresolved.isEmpty());
    }

    /** Checks one face's own ability lines and every SVar body it defines. */
    private static void checkFace(String filename, ICardFace face, List<String> unresolved) {
        Set<String> defined = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        List<String> lines = new ArrayList<>();
        addAll(lines, face.getAbilities());
        addAll(lines, face.getTriggers());
        addAll(lines, face.getStaticAbilities());
        addAll(lines, face.getReplacements());
        if (face.getVariables() != null) {
            for (Map.Entry<String, String> svar : face.getVariables()) {
                defined.add(svar.getKey());
                lines.add(svar.getValue());
            }
        }

        for (String line : lines) {
            // The same map Forge builds when it creates the ability, so a repeated key resolves to
            // the value Forge actually uses.
            Map<String, String> params = AbilityFactory.getMapParams(line);
            for (String name : referenced(params)) {
                if (!defined.contains(name)) {
                    unresolved.add(filename + ": " + face.getName() + " names SVar '" + name
                            + "', which it does not define");
                }
            }
        }
    }

    /** The SVar names a param map refers to, in all four shapes AbilityFactory reads. */
    private static List<String> referenced(Map<String, String> params) {
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (SUB_ABILITY_KEYS.contains(param.getKey())) {
                names.add(param.getValue().trim());
            }
        }

        String api = api(params);
        if (CHOICE_APIS.contains(api) && params.containsKey("Choices")) {
            names.addAll(split(params.get("Choices")));
        }
        if ("RollDice".equals(api) && params.containsKey("ResultSubAbilities")) {
            for (String pair : split(params.get("ResultSubAbilities"))) {
                int colon = pair.indexOf(':');
                if (colon > 0) {
                    names.add(pair.substring(colon + 1).trim());
                }
            }
        }
        names.removeIf(String::isEmpty);
        return names;
    }

    private static String api(Map<String, String> params) {
        for (String key : API_KEYS) {
            if (params.containsKey(key)) {
                return params.get(key);
            }
        }
        return "";
    }

    private static List<String> split(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toList());
    }

    private static void addAll(List<String> lines, Iterable<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            lines.add(value);
        }
    }

    private static List<Path> scripts(Path cardsfolder) throws IOException {
        try (Stream<Path> walk = Files.walk(cardsfolder)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".txt"))
                    .sorted(Comparator.comparing(Path::toString))
                    .collect(Collectors.toList());
        }
    }

    /** Resolves the corpus whether the tests run from the module or the repository root. */
    private static Path findCardsfolder() {
        for (Path candidate : List.of(
                Path.of("..", "forge-gui", "res", "cardsfolder"),
                Path.of("forge-gui", "res", "cardsfolder"))) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new SkipException("cardsfolder not found relative to " + Path.of("").toAbsolutePath());
    }
}
