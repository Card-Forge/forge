import forge.CardStorageReader;
import forge.StaticData;
import forge.card.CardRarity;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.item.PaperCard;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

/** Generates legal Pauper Commander (PDH) starter decks from Forge's real common pool. */
public class PdhDeckGen {
    public static void main(String[] args) throws Exception {
        forge.util.Lang.createInstance("en-US");
        forge.util.Localizer.getInstance().initialize("en-US", "res/languages/");
        forge.ImageKeys.initializeDirs("res/pics/cards/", new HashMap<>(),
                "res/pics/tokens/", "res/pics/icons/", "res/pics/boosters/",
                "res/pics/fatpacks/", "res/pics/boosterboxes/", "res/pics/precons/",
                "res/pics/tournamentpacks/");
        CardStorageReader reader = new CardStorageReader("res/cardsfolder/", null, false);
        StaticData sd = new StaticData(reader, null, "res/editions/", "res/customeditions/",
                "res/blockdata/", "Latest Art All Editions", true, false);
        DeckFormat pdh = DeckFormat.PauperCommander;

        // Build per-name pools, choosing one representative printing per card name.
        Map<String, PaperCard> commonNonland = new LinkedHashMap<>();   // legal 99 cards (commons, not lands)
        Map<String, PaperCard> uncommonCmd = new LinkedHashMap<>();     // legal commanders (uncommon creature/vehicle/spacecraft)
        for (PaperCard c : sd.getCommonCards().getAllCards()) {
            if (c.isRebalanced() || c.getName().startsWith("A-")) continue; // skip Alchemy/digital-rebalanced
            CardType t = c.getRules().getType();
            if (c.getRarity() == CardRarity.Common && !t.isLand() && pdh.isLegalCard(c)) {
                commonNonland.putIfAbsent(c.getName(), c);
            }
            if (pdh.isLegalCommander(c)) {
                uncommonCmd.putIfAbsent(c.getName(), c);
            }
        }

        String[] colorNames = {"White", "Blue", "Black", "Red", "Green"};
        byte[] masks = {MagicColor.WHITE, MagicColor.BLUE, MagicColor.BLACK, MagicColor.RED, MagicColor.GREEN};
        String[] basics = {"Plains", "Island", "Swamp", "Mountain", "Forest"};

        File outDir = new File("pdh-decks");
        outDir.mkdirs();
        int made = 0;

        for (int i = 0; i < masks.length; i++) {
            final byte mask = masks[i];
            // Pick a mono-colored commander: prefer a creature, then vehicle/spacecraft.
            PaperCard commander = pickCommander(uncommonCmd.values(), mask, true);
            if (commander == null) commander = pickCommander(uncommonCmd.values(), mask, false);
            if (commander == null) { System.out.println(colorNames[i] + ": no commander found, skipping"); continue; }

            // Candidate 99: commons whose colour identity fits the mono commander; creatures first.
            List<PaperCard> creatures = new ArrayList<>();
            List<PaperCard> others = new ArrayList<>();
            for (PaperCard c : commonNonland.values()) {
                if (c.getName().equals(commander.getName())) continue;
                ColorSet ci = c.getRules().getColorIdentity();
                if (!ci.hasNoColorsExcept(mask)) continue; // includes colourless commons
                if (c.getRules().getType().isCreature()) creatures.add(c); else others.add(c);
            }
            // Shuffle (seeded per colour) for a natural spread instead of an alphabetical pile.
            creatures.sort(Comparator.comparing(PaperCard::getName));
            others.sort(Comparator.comparing(PaperCard::getName));
            Collections.shuffle(creatures, new Random(1000 + i));
            Collections.shuffle(others, new Random(2000 + i));

            // Creature-heavy starter: ~45 creatures + ~17 non-creature commons (removal/draw/utility).
            List<PaperCard> nonland = new ArrayList<>();
            for (PaperCard c : creatures) { if (nonland.size() >= 45) break; nonland.add(c); }
            for (PaperCard c : others)   { if (nonland.size() >= 62) break; nonland.add(c); }
            for (PaperCard c : creatures) { if (nonland.size() >= 62) break; if (!nonland.contains(c)) nonland.add(c); }

            int landCount = 99 - nonland.size();
            PaperCard basic = sd.getCommonCards().getCard(basics[i]);

            // Assemble and validate.
            Deck deck = new Deck("PDH Starter - " + colorNames[i] + " (" + commander.getName() + ")");
            for (PaperCard c : nonland) deck.getMain().add(c, 1);
            deck.getMain().add(basic, landCount);
            deck.getOrCreate(DeckSection.Commander).add(commander, 1);

            String problem = pdh.getDeckConformanceProblem(deck);
            if (problem != null) {
                System.out.println(colorNames[i] + ": ILLEGAL -> " + problem);
                continue;
            }

            writeDeck(new File(outDir, "PDH Starter - " + colorNames[i] + ".dck"), deck.getName(),
                    commander, nonland, basic, landCount);
            made++;
            System.out.println(colorNames[i] + ": OK  commander=" + commander.getName()
                    + "  99=" + nonland.size() + " commons + " + landCount + " " + basics[i]);
        }
        System.out.println("Generated " + made + " legal PDH starter decks in " + outDir.getAbsolutePath());
    }

    private static PaperCard pickCommander(Collection<PaperCard> pool, byte mask, boolean creatureOnly) {
        List<PaperCard> matches = new ArrayList<>();
        for (PaperCard c : pool) {
            if (c.getRules().getColorIdentity().getColor() != mask) continue; // exactly this mono colour
            if (creatureOnly && !c.getRules().getType().isCreature()) continue;
            matches.add(c);
        }
        if (matches.isEmpty()) return null;
        // Prefer a mid-cost creature with the most rules text (a build-around feel); tie-break by name.
        matches.sort((a, b) -> {
            int ca = commanderScore(a), cb = commanderScore(b);
            if (ca != cb) return Integer.compare(cb, ca);
            return a.getName().compareTo(b.getName());
        });
        return matches.get(0);
    }

    private static int commanderScore(PaperCard c) {
        int cmc = c.getRules().getManaCost().getCMC();
        int textLen = c.getRules().getMainPart().getOracleText() == null ? 0
                : c.getRules().getMainPart().getOracleText().length();
        int score = textLen;
        if (cmc >= 3 && cmc <= 6) score += 200; // sweet spot for a commander
        return score;
    }

    private static void writeDeck(File f, String name, PaperCard cmd, List<PaperCard> nonland,
                                  PaperCard basic, int landCount) throws Exception {
        try (PrintWriter w = new PrintWriter(f, "UTF-8")) {
            w.println("[metadata]");
            w.println("Name=" + name);
            w.println("[Commander]");
            w.println("1 " + ref(cmd));
            w.println("[Main]");
            for (PaperCard c : nonland) w.println("1 " + ref(c));
            w.println(landCount + " " + ref(basic));
        }
    }

    private static String ref(PaperCard c) {
        return c.getName() + "|" + c.getEdition() + "|" + c.getCollectorNumber();
    }
}
