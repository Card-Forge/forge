package crucible.oracle;

import forge.card.CardRules;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.ICardFace;
import forge.util.FileSection;
import forge.util.FileUtil;
import forge.util.Lang;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Dumps every card in Forge's corpus as one line of canonical JSON, for the Go
 * port to be diffed against. This is the P1 gate of ADR-0010: Forge's own
 * reader produces the reference, so a parser divergence shows up as a diff
 * rather than as a rules bug six milestones later.
 *
 * <p>The format is written by hand, field by field, and mirrored exactly by
 * {@code Card.CanonicalJSON} in {@code crucible/internal/carddb}. A JSON
 * library is deliberately not used: the two sides have to agree on escaping,
 * key order and number formatting, and no two libraries do.
 *
 * <p>Usage:
 *
 * <pre>
 * java -cp ... crucible.oracle.CardRulesDumper &lt;cardsfolder&gt; &lt;TypeLists.txt&gt;
 * </pre>
 */
public final class CardRulesDumper {

    private CardRulesDumper() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("usage: CardRulesDumper <cardsfolder> <TypeLists.txt>");
            System.exit(2);
        }
        // CardFace.assignMissingFieldsToVariant reaches for the localisation
        // singleton when a functional variant has a flavour name, and Forge
        // initialises it in forge-gui. Without this the dump dies 643 cards in.
        Lang.createInstance("en-US");
        loadTypeLists(Path.of(args[1]));

        PrintStream out = new PrintStream(System.out, false, StandardCharsets.UTF_8);
        for (Path script : scriptsSortedByName(Path.of(args[0]))) {
            String filename = script.getFileName().toString().replaceFirst("\\.txt$", "");
            List<String> lines = Files.readAllLines(script, StandardCharsets.UTF_8);
            CardRules rules = new CardRules.Reader().readCard(lines, filename);
            out.println(canonicalJson(rules, filename));
        }
        out.flush();
    }

    /**
     * Loads the subtype vocabulary the way FModel.loadDynamicGamedata does,
     * without depending on forge-gui: CardType.parse silently drops every
     * subtype when this has not run, which would make the dump wrong rather
     * than fail.
     */
    private static void loadTypeLists(Path typeLists) {
        Map<String, List<String>> sections = FileSection.parseSections(FileUtil.readFile(typeLists.toFile()));
        for (Map.Entry<String, List<String>> section : sections.entrySet()) {
            CardType.Helper.parseTypes(section.getKey(), section.getValue());
        }
        if (CardType.getSortedSubTypes().isEmpty()) {
            throw new IllegalStateException("no subtypes loaded from " + typeLists);
        }
    }

    private static List<Path> scriptsSortedByName(Path cardsfolder) throws IOException {
        try (Stream<Path> walk = Files.walk(cardsfolder)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".txt"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
        }
    }

    // Field order below is the contract. Keep it identical to dump.go.
    private static String canonicalJson(CardRules rules, String filename) {
        StringBuilder b = new StringBuilder(1024);
        b.append("{\"file\":");
        writeString(b, filename);
        b.append(",\"split\":");
        writeString(b, rules.getSplitType().name());
        b.append(",\"partnerWith\":");
        writeString(b, rules.getPartnerWith());
        b.append(",\"meldWith\":");
        writeString(b, rules.getMeldWith());
        b.append(",\"remAI\":").append(rules.getAiHints().getRemAIDecks());
        b.append(",\"remRandom\":").append(rules.getAiHints().getRemRandomDecks());
        b.append(",\"remNonCommander\":").append(rules.getAiHints().getRemNonCommanderDecks());

        b.append(",\"placeholders\":[");
        boolean firstPlaceholder = true;
        for (String name : new TreeSet<>(rules.getPlaceholderFaceNames())) {
            if (!firstPlaceholder) {
                b.append(',');
            }
            firstPlaceholder = false;
            writeString(b, name);
        }
        b.append("],\"faces\":[");
        boolean first = true;
        for (int index = 0; index < FACE_ACCESSORS.size(); index++) {
            ICardFace face = FACE_ACCESSORS.get(index).apply(rules);
            if (face == null) {
                continue;
            }
            if (!first) {
                b.append(',');
            }
            first = false;
            writeFace(b, face, index);
        }
        return b.append("]}").toString();
    }

    private interface FaceAccessor {
        ICardFace apply(CardRules rules);
    }

    /** Face index to accessor, matching carddb's Face* constants exactly. */
    private static final List<FaceAccessor> FACE_ACCESSORS = List.of(
            CardRules::getMainPart,
            CardRules::getOtherPart,
            CardRules::getWSpecialize,
            CardRules::getUSpecialize,
            CardRules::getBSpecialize,
            CardRules::getRSpecialize,
            CardRules::getGSpecialize);

    private static void writeFace(StringBuilder b, ICardFace face, int index) {
        b.append("{\"i\":").append(index);
        b.append(",\"name\":");
        writeString(b, face.getName());
        b.append(",\"flavorName\":");
        writeString(b, face.getFlavorName());
        b.append(",\"type\":");
        writeString(b, face.getType() == null ? "" : face.getType().toString());
        b.append(",\"manaCost\":");
        writeString(b, face.getManaCost().toString());
        b.append(",\"colors\":").append(colorMask(face.getColor()));
        b.append(",\"power\":");
        writeString(b, face.getPower());
        b.append(",\"toughness\":");
        writeString(b, face.getToughness());
        b.append(",\"loyalty\":");
        writeString(b, face.getInitialLoyalty());
        b.append(",\"defense\":");
        writeString(b, face.getDefense());
        b.append(",\"lights\":");
        writeString(b, lights(face.getAttractionLights()));
        b.append(",\"text\":");
        writeString(b, face.getNonAbilityText());
        b.append(",\"oracle\":");
        writeString(b, face.getOracleText());

        writeStrings(b, "abilities", face.getAbilities());
        writeStrings(b, "keywords", face.getKeywords());
        writeStrings(b, "triggers", face.getTriggers());
        writeStrings(b, "statics", face.getStaticAbilities());
        writeStrings(b, "replacements", face.getReplacements());
        writeStrings(b, "deckRules", face.getDeckRules());
        writeStrings(b, "draftActions", face.getDraftActions());

        b.append(",\"svars\":[");
        boolean firstVar = true;
        Iterable<Map.Entry<String, String>> variables =
                face.getVariables() == null ? List.<Map.Entry<String, String>>of() : face.getVariables();
        for (Map.Entry<String, String> svar : variables) {
            if (!firstVar) {
                b.append(',');
            }
            firstVar = false;
            b.append('[');
            writeString(b, svar.getKey());
            b.append(',');
            writeString(b, svar.getValue());
            b.append(']');
        }

        b.append("],\"variants\":[");
        if (face.hasFunctionalVariants()) {
            boolean firstVariant = true;
            for (String name : new TreeSet<>(face.getFunctionalVariants().keySet())) {
                if (!firstVariant) {
                    b.append(',');
                }
                firstVariant = false;
                writeString(b, name);
            }
        }
        b.append("]}");
    }

    /** A colour mask rather than a name: ColorSet spells WG as "GW". */
    private static int colorMask(ColorSet color) {
        return color == null ? 0 : color.getColor() & 0xFF;
    }

    /** Lights sorted numerically, which is all a Set can promise. */
    private static String lights(Set<Integer> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        List<Integer> sorted = new ArrayList<>(new TreeSet<>(values));
        return sorted.stream().map(String::valueOf).collect(Collectors.joining(" "));
    }

    /** Null-tolerant: CardFace returns the raw field, which is null until the first add. */
    private static void writeStrings(StringBuilder b, String key, Iterable<String> values) {
        b.append(",\"").append(key).append("\":[");
        boolean first = true;
        for (String value : values == null ? List.<String>of() : values) {
            if (!first) {
                b.append(',');
            }
            first = false;
            writeString(b, value);
        }
        b.append(']');
    }

    /** Escapes exactly what JSON requires, matching writeJSONString in dump.go. */
    private static void writeString(StringBuilder b, String s) {
        b.append('"');
        if (s != null) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"' -> b.append("\\\"");
                    case '\\' -> b.append("\\\\");
                    case '\n' -> b.append("\\n");
                    case '\r' -> b.append("\\r");
                    case '\t' -> b.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            b.append(String.format("\\u%04x", (int) c));
                        } else {
                            b.append(c);
                        }
                    }
                }
            }
        }
        b.append('"');
    }
}
