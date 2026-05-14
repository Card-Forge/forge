package forge.deck;

import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.util.FileUtil;
import forge.util.Localizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

public final class CommanderBracketCalculator {
    private static final String LATE_GAME = "late_game";
    private static final String EARLY_GAME = "early_game";

    private static final Data DATA = new Data();

    private CommanderBracketCalculator() {
    }

    public static Result calculate(final Deck deck) {
        if (deck == null) {
            return Result.empty();
        }

        final Set<String> deckCards = getDeckCardNames(deck);
        final List<String> gamechangers = DATA.findCards(deckCards, DATA.gamechangers);
        final List<String> massLandDenial = DATA.findCards(deckCards, DATA.massLandDenial);
        final List<String> extraTurns = DATA.findCards(deckCards, DATA.extraTurns);
        final List<String> chainedExtraTurns = DATA.findCards(deckCards, DATA.chainedExtraTurns);
        final List<Combo> lateGameCombos = DATA.findCombos(deckCards, LATE_GAME);
        final List<Combo> earlyGameCombos = DATA.findCombos(deckCards, EARLY_GAME);

        int bracket = 1;
        if (!massLandDenial.isEmpty()) {
            bracket = Math.max(bracket, 4);
        }
        if (!chainedExtraTurns.isEmpty()) {
            bracket = Math.max(bracket, 4);
        }
        if (extraTurns.size() >= 4) {
            bracket = Math.max(bracket, 4);
        }
        else if (extraTurns.size() >= 3) {
            bracket = Math.max(bracket, 3);
        }
        else if (extraTurns.size() >= 2) {
            bracket = Math.max(bracket, 2);
        }
        if (gamechangers.size() >= 4) {
            bracket = Math.max(bracket, 4);
        }
        else if (!gamechangers.isEmpty()) {
            bracket = Math.max(bracket, 3);
        }
        if (!lateGameCombos.isEmpty()) {
            bracket = Math.max(bracket, 3);
        }
        if (!earlyGameCombos.isEmpty()) {
            bracket = Math.max(bracket, 4);
        }

        return new Result(bracket, gamechangers, massLandDenial, extraTurns, chainedExtraTurns, lateGameCombos, earlyGameCombos);
    }

    public static int getBracket(final Deck deck) {
        return calculate(deck).getBracket();
    }

    public static String getDisplayBracket(final Deck deck) {
        return String.valueOf(getBracket(deck));
    }

    public static String getExplanation(final Deck deck) {
        return calculate(deck).toExplanation();
    }

    private static Set<String> getDeckCardNames(final Deck deck) {
        final Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (final Entry<PaperCard, Integer> cardEntry : deck.getAllCardsInASinglePool()) {
            result.addAll(cardEntry.getKey().getAllSearchableNames());
        }
        return result;
    }

    private static String normalize(final String text) {
        return text.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Data {
        private final Map<String, String> gamechangers = readCardList(ForgeConstants.COMMANDER_BRACKET_GAMECHANGERS_FILE);
        private final Map<String, String> massLandDenial = readCardList(ForgeConstants.COMMANDER_BRACKET_MASS_LAND_DENIAL_FILE);
        private final Map<String, String> extraTurns = readCardList(ForgeConstants.COMMANDER_BRACKET_EXTRA_TURNS_FILE);
        private final Map<String, String> chainedExtraTurns = readCardList(ForgeConstants.COMMANDER_BRACKET_CHAINED_EXTRA_TURNS_FILE);
        private final List<Combo> combos = readCombos();

        private List<String> findCards(final Set<String> deckCards, final Map<String, String> source) {
            final List<String> result = new ArrayList<>();
            for (final String cardName : deckCards) {
                final String match = source.get(normalize(cardName));
                if (match != null) {
                    result.add(match);
                }
            }
            Collections.sort(result);
            return result;
        }

        private List<Combo> findCombos(final Set<String> deckCards, final String category) {
            final List<Combo> result = new ArrayList<>();
            for (final Combo combo : combos) {
                if (category.equalsIgnoreCase(combo.category)
                        && deckCards.contains(combo.card1)
                        && deckCards.contains(combo.card2)) {
                    result.add(combo);
                }
            }
            result.sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
            return result;
        }

        private static Map<String, String> readCardList(final String filename) {
            final Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (final String line : FileUtil.readFile(filename)) {
                final String cardName = stripComment(line).trim();
                if (!cardName.isEmpty()) {
                    result.put(normalize(cardName), cardName);
                }
            }
            return result;
        }

        private static List<Combo> readCombos() {
            final List<Combo> result = new ArrayList<>();
            for (final String line : FileUtil.readFile(ForgeConstants.COMMANDER_BRACKET_COMBOS_FILE)) {
                final String comboLine = stripComment(line).trim();
                if (comboLine.isEmpty()) {
                    continue;
                }
                final String[] parts = comboLine.split("\\s*\\|\\s*");
                if (parts.length >= 3) {
                    result.add(new Combo(parts[0].trim(), parts[1].trim(), parts[2].trim()));
                }
            }
            return result;
        }

        private static String stripComment(final String line) {
            final int commentIndex = line.indexOf('#');
            return commentIndex < 0 ? line : line.substring(0, commentIndex);
        }
    }

    public static final class Combo {
        private final String category;
        private final String card1;
        private final String card2;

        private Combo(final String category, final String card1, final String card2) {
            this.category = category;
            this.card1 = card1;
            this.card2 = card2;
        }

        public String getCategory() {
            return category;
        }

        public String getCard1() {
            return card1;
        }

        public String getCard2() {
            return card2;
        }

        @Override
        public String toString() {
            return card1 + " + " + card2;
        }
    }

    public static final class Result {
        private final int bracket;
        private final List<String> gamechangers;
        private final List<String> massLandDenial;
        private final List<String> extraTurns;
        private final List<String> chainedExtraTurns;
        private final List<Combo> lateGameCombos;
        private final List<Combo> earlyGameCombos;

        private Result(final int bracket, final List<String> gamechangers, final List<String> massLandDenial,
                       final List<String> extraTurns, final List<String> chainedExtraTurns,
                       final List<Combo> lateGameCombos, final List<Combo> earlyGameCombos) {
            this.bracket = bracket;
            this.gamechangers = gamechangers;
            this.massLandDenial = massLandDenial;
            this.extraTurns = extraTurns;
            this.chainedExtraTurns = chainedExtraTurns;
            this.lateGameCombos = lateGameCombos;
            this.earlyGameCombos = earlyGameCombos;
        }

        private static Result empty() {
            return new Result(1, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }

        public int getBracket() {
            return bracket;
        }

        public String toExplanation() {
            final Localizer localizer = Localizer.getInstance();
            final StringBuilder sb = new StringBuilder();
            sb.append(localizer.getMessage("lblCommanderBracketMinimum", bracket)).append("\n\n");
            appendCards(sb, localizer, localizer.getMessage("lblCommanderBracketGameChangers"), gamechangers,
                    gamechangers.size() >= 4 ? localizer.getMessage("lblCommanderBracketReasonGameChangersFour")
                            : !gamechangers.isEmpty() ? localizer.getMessage("lblCommanderBracketReasonGameChangersOne")
                            : null);
            appendCards(sb, localizer, localizer.getMessage("lblCommanderBracketMassLandDenial"), massLandDenial,
                    massLandDenial.isEmpty() ? null : localizer.getMessage("lblCommanderBracketReasonMassLandDenial"));
            appendCards(sb, localizer, localizer.getMessage("lblCommanderBracketExtraTurns"), extraTurns,
                    extraTurns.size() >= 4 ? localizer.getMessage("lblCommanderBracketReasonExtraTurnsFour")
                            : extraTurns.size() >= 3 ? localizer.getMessage("lblCommanderBracketReasonExtraTurnsThree")
                            : extraTurns.size() >= 2 ? localizer.getMessage("lblCommanderBracketReasonExtraTurnsTwo")
                            : extraTurns.isEmpty() ? null : localizer.getMessage("lblCommanderBracketReasonExtraTurnsFew"));
            appendCards(sb, localizer, localizer.getMessage("lblCommanderBracketChainedExtraTurns"), chainedExtraTurns,
                    chainedExtraTurns.isEmpty() ? null : localizer.getMessage("lblCommanderBracketReasonChainedExtraTurn"));
            appendCombos(sb, localizer, lateGameCombos, earlyGameCombos);
            return sb.toString();
        }

        private static void appendCards(final StringBuilder sb, final Localizer localizer,
                                        final String title, final List<String> cards, final String reason) {
            sb.append(title).append("\n");
            if (cards.isEmpty()) {
                sb.append("  ").append(localizer.getMessage("lblNone")).append("\n");
            }
            else {
                for (final String card : cards) {
                    sb.append("  ").append(card).append("\n");
                }
            }
            if (reason != null) {
                sb.append("  ").append(reason).append("\n");
            }
            sb.append("\n");
        }

        private static void appendCombos(final StringBuilder sb, final Localizer localizer,
                                         final List<Combo> lateGameCombos, final List<Combo> earlyGameCombos) {
            sb.append(localizer.getMessage("lblCommanderBracketTwoCardCombos")).append("\n");
            if (lateGameCombos.isEmpty() && earlyGameCombos.isEmpty()) {
                sb.append("  ").append(localizer.getMessage("lblNone")).append("\n");
            }
            else {
                for (final Combo combo : lateGameCombos) {
                    sb.append("  ").append(localizer.getMessage("lblCommanderBracketLateGame")).append(": ").append(combo).append("\n");
                }
                for (final Combo combo : earlyGameCombos) {
                    sb.append("  ").append(localizer.getMessage("lblCommanderBracketEarlyGame")).append(": ").append(combo).append("\n");
                }
                if (!lateGameCombos.isEmpty()) {
                    sb.append("  ").append(localizer.getMessage("lblCommanderBracketReasonLateGameCombo")).append("\n");
                }
                if (!earlyGameCombos.isEmpty()) {
                    sb.append("  ").append(localizer.getMessage("lblCommanderBracketReasonEarlyGameCombo")).append("\n");
                }
            }
        }
    }
}
