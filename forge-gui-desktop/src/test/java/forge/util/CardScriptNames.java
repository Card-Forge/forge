package forge.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import forge.localinstance.properties.ForgeConstants;

/** Card names read straight from the card scripts, scanned once for every data test that checks names. */
public final class CardScriptNames {
    private static final String NAME_PREFIX = "Name:";
    private static final String FLAVOR_NAME_PREFIX = "FlavorName:";

    private static Set<String> names;
    private static Set<String> namesWithFlavor;

    private CardScriptNames() { }

    /** Each face name, plus "A // B" for multi-face cards; case-insensitive. */
    public static synchronized Set<String> names() {
        scan();
        return names;
    }

    /** As {@link #names()}, plus every flavor name. */
    public static synchronized Set<String> namesWithFlavor() {
        scan();
        return namesWithFlavor;
    }

    private record Script(List<String> faces, List<String> flavorNames) { }

    private static void scan() {
        if (names != null) {
            return;
        }
        final List<Script> scripts;
        try (Stream<Path> paths = Files.walk(Path.of(ForgeConstants.CARD_DATA_DIR))) {
            scripts = paths.filter(p -> p.toString().endsWith(".txt")).collect(Collectors.toList())
                    .parallelStream().map(CardScriptNames::read).collect(Collectors.toList());
        } catch (final IOException e) {
            throw new UncheckedIOException("Could not read card names from " + ForgeConstants.CARD_DATA_DIR, e);
        }
        final Set<String> plain = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        final Set<String> flavored = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (final Script s : scripts) {
            plain.addAll(s.faces());
            if (s.faces().size() > 1) {
                plain.add(String.join(" // ", s.faces()));
            }
            flavored.addAll(s.flavorNames());
        }
        flavored.addAll(plain);
        names = Collections.unmodifiableSet(plain);
        namesWithFlavor = Collections.unmodifiableSet(flavored);
    }

    private static Script read(final Path path) {
        final List<String> faces = new ArrayList<>();
        final List<String> flavorNames = new ArrayList<>();
        for (final String line : FileUtil.readFile(path.toString())) {
            if (line.startsWith(NAME_PREFIX)) {
                faces.add(line.substring(NAME_PREFIX.length()).trim());
            } else if (line.contains(FLAVOR_NAME_PREFIX)) {
                flavorNames.add(line.substring(line.indexOf(FLAVOR_NAME_PREFIX) + FLAVOR_NAME_PREFIX.length()).trim());
            }
        }
        return new Script(faces, flavorNames);
    }
}
