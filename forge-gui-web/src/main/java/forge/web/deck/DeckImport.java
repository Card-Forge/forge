package forge.web.deck;

import forge.card.CardRules;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckRecognizer;
import forge.deck.DeckRecognizer.Token;
import forge.deck.DeckRecognizer.TokenType;
import forge.deck.DeckSection;
import forge.deck.DeckUrlLoader;
import forge.item.PaperCard;
import forge.util.Localizer;
import forge.web.protocol.ToBrowser.Fetched;
import forge.web.protocol.ToBrowser.ImportFix;
import forge.web.protocol.ToBrowser.ImportFixKind;
import forge.web.protocol.ToBrowser.ImportLine;
import forge.web.protocol.ToBrowser.ImportLineKind;
import forge.web.protocol.ToBrowser.ImportProblem;
import forge.web.protocol.ToBrowser.ImportResult;
import forge.web.protocol.ToBrowser.ImportSummary;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads a deck list for the browser. It asks Legality whether each card is allowed, so the line marks agree with the editor's verdict.
 * It reads line by line instead of through DeckImportController. The page marks and fixes each source line, and the
 * recognizer's tokens do not keep their line positions.
 */
public final class DeckImport {
    private static final int MOST_COMMANDER_CHOICES = 4;
    private static final Set<TokenType> CARDS = EnumSet.of(TokenType.LEGAL_CARD, TokenType.LIMITED_CARD,
            TokenType.CARD_FROM_NOT_ALLOWED_SET, TokenType.CARD_FROM_INVALID_SET);

    private DeckImport() {
    }

    /** What reading a list found, and the deck it makes. commanderChosen is set when the commander was picked because only one card could be the commander. */
    public record Read(List<ImportLine> lines, List<ImportProblem> problems, Deck deck, String name, String commanderChosen,
            int notImported, Legality.Result legality) {
    }

    public static Read read(final String text, final LegalityScope check) {
        final String[] raw = text.split("\r?\n", -1);
        // Lines that the recognizer misses only because of a set code and collector number are read by card name
        final String[] lines = DeckUrlLoader.getRecognizableImportLines(new DeckRecognizer(), String.join("\n", raw));
        final DeckRecognizer recognizer = new DeckRecognizer();
        recognizer.forceImportBannedAndRestrictedCards();
        final Deck deck = new Deck("");
        final ImportLineKind[] kinds = new ImportLineKind[raw.length];
        final Map<String, Integer> lineOf = new HashMap<>();
        final List<ImportProblem> problems = new ArrayList<>();
        String name = null;
        int notImported = 0;
        // Reading starts in the main deck. A legendary card before any heading stays there, and chooseCommander picks the commander later
        DeckSection section = DeckSection.Main;
        // The recognizer cannot read a deck file's card lines, so each is parsed alone as DeckSerializer parses it
        final boolean file = Arrays.stream(raw).anyMatch(l -> l.trim().equalsIgnoreCase("[metadata]"));
        DeckSection fileSection = null;
        boolean fileHead = false;
        for (int i = 0; i < raw.length; i++) {
            final String line = raw[i].trim();
            final CardPool listed = new CardPool();
            final DeckSection to;
            if (file) {
                if (line.startsWith("[") && line.endsWith("]")) {
                    final String heading = line.substring(1, line.length() - 1);
                    fileSection = DeckSection.smartValueOf(heading);
                    fileHead = "metadata".equalsIgnoreCase(heading);
                    kinds[i] = fileSection == null ? ImportLineKind.ignored : ImportLineKind.heading;
                    continue;
                }
                if (fileHead && line.startsWith("Name=")) {
                    kinds[i] = ImportLineKind.heading;
                    name = line.substring("Name=".length());
                    continue;
                }
                if (fileSection != null) {
                    listed.addAll(CardPool.fromCardList(List.of(line)));
                }
                if (listed.isEmpty()) {
                    kinds[i] = ImportLineKind.ignored;
                    continue;
                }
                // Forge reads a card it does not have as a placeholder that no game can play
                if (listed.toFlatList().get(0).getRules().isUnsupported()) {
                    kinds[i] = ImportLineKind.problem;
                    notImported++;
                    problems.add(unknown(i, line.replaceFirst("\\|.*$", ""), false));
                    continue;
                }
                to = fileSection;
            } else {
                if (line.startsWith("//")) {
                    // A comment, unless it is the "// Name:" line that some sites put first
                    final String named = DeckRecognizer.deckNameMatch(line);
                    kinds[i] = named.isEmpty() ? ImportLineKind.ignored : ImportLineKind.heading;
                    name = named.isEmpty() ? name : named;
                    continue;
                }
                final Token token = i < lines.length ? recognizer.recognizeLine(lines[i], section) : null;
                if (token == null) {
                    kinds[i] = ImportLineKind.ignored;
                    continue;
                }
                switch (token.getType()) {
                    case DECK_NAME -> {
                        kinds[i] = ImportLineKind.heading;
                        name = token.getText();
                        continue;
                    }
                    case DECK_SECTION_NAME -> {
                        kinds[i] = ImportLineKind.heading;
                        section = DeckSection.valueOf(token.getText());
                        continue;
                    }
                    case UNKNOWN_CARD, UNSUPPORTED_CARD -> {
                        kinds[i] = ImportLineKind.problem;
                        notImported++;
                        problems.add(unknown(i, line, token.getType() == TokenType.UNSUPPORTED_CARD));
                        continue;
                    }
                    default -> {
                        if (!CARDS.contains(token.getType())) {
                            kinds[i] = ImportLineKind.ignored;
                            continue;
                        }
                        listed.add(token.getCard(), token.getQuantity());
                        to = token.getTokenSection();
                    }
                }
            }
            final PaperCard first = listed.toFlatList().get(0);
            kinds[i] = ImportLineKind.read;
            deck.getOrCreate(to).addAll(listed);
            lineOf.putIfAbsent(first.getName(), i);
        }
        final String chosen = chooseCommander(deck, check, problems);
        final Legality.Result legality = Legality.check(deck, check);
        final List<ImportProblem> cardProblems = new ArrayList<>();
        legality.flags().forEach((card, flag) -> {
            final Integer at = lineOf.get(card);
            if (at != null) {
                kinds[at] = ImportLineKind.problem;
                cardProblems.add(new ImportProblem(at, Localizer.getInstance().getMessage("lblWebImportLineCard", at + 1, card),
                        StringUtils.capitalize(flag) + ".",
                        List.of(new ImportFix(ImportFixKind.leaveOut, Localizer.getInstance().getMessage("lblWebImportLeaveOut"), null))));
            }
        });
        problems.addAll(cardProblems);
        problems.sort((a, b) -> Integer.compare(a.line(), b.line()));
        final List<ImportLine> marks = new ArrayList<>();
        for (final ImportLineKind kind : kinds) {
            marks.add(new ImportLine(kind));
        }
        deck.setName(name == null ? "" : name.trim());
        return new Read(marks, problems, deck, name == null ? null : name.trim(), chosen, notImported, legality);
    }

    static ImportResult result(final int request, final Read read, final String name, final LegalityScope check, final Fetched fetched) {
        final Deck deck = read.deck();
        final Legality.Result legality = read.legality();
        final List<PaperCard> commanders = Legality.commanders(deck, check);
        final CardPool sideboard = deck.get(DeckSection.Sideboard);
        final String colours = commanders.isEmpty() ? DeckFacts.colors(deck) : Legality.identityLetters(commanders);
        final ImportSummary summary = new ImportSummary(
                DeckFacts.played(deck),
                sideboard == null ? 0 : sideboard.countAll(), read.notImported(),
                commanders.isEmpty() ? null : commanders.get(0).getName(), read.commanderChosen() != null,
                colours, legality.verdict(), CardCatalog.groups(deck.getMain(), legality),
                CardCatalog.cards(sideboard, legality));
        return new ImportResult(request, read.lines(), read.problems(), summary, name, fetched);
    }

    /** A line that names no card. The closest known name is offered when it is close enough to be the intended card. */
    private static ImportProblem unknown(final int line, final String text, final boolean unsupported) {
        final String title = Localizer.getInstance().getMessage("lblWebImportLineText", line + 1, text);
        if (unsupported) {
            return new ImportProblem(line, title, Localizer.getInstance().getMessage("lblWebImportUnsupported"), List.of(new ImportFix(ImportFixKind.leaveOut, Localizer.getInstance().getMessage("lblWebImportLeaveOut"), null)));
        }
        final String closest = CardCatalog.get().closestName(text.replaceFirst("^\\s*\\d+x?\\s+", "").replaceFirst("\\s*[\\[(].*$", ""));
        final List<ImportFix> fixes = new ArrayList<>();
        if (closest != null) {
            fixes.add(new ImportFix(ImportFixKind.use, Localizer.getInstance().getMessage("lblWebImportUse", closest), closest));
        }
        fixes.add(new ImportFix(ImportFixKind.leaveOut, Localizer.getInstance().getMessage("lblWebImportLeaveOut"), null));
        return new ImportProblem(line, title, Localizer.getInstance().getMessage("lblWebImportNotACard"), fixes);
    }

    /**
     * With no Commander section, a single card that could lead the deck becomes its commander, and its name is returned. Several candidates are offered as a choice.
     * As in DeckImportController, the commander is looked for in the sideboard, or in the main deck when there is no sideboard. Unlike there, a candidate must be a
     * legal commander in this format, because the test DeckImportController uses also accepts every instant and sorcery as a possible signature spell.
     */
    private static String chooseCommander(final Deck deck, final LegalityScope check, final List<ImportProblem> problems) {
        final DeckFormat df = check.deckFormat();
        final CardPool commanders = deck.get(DeckSection.Commander);
        if (!df.hasCommander() || (commanders != null && !commanders.isEmpty())) {
            return null;
        }
        final CardPool side = deck.get(DeckSection.Sideboard);
        final CardPool from = side != null && !side.isEmpty() ? side : deck.getMain();
        final List<PaperCard> candidates = new ArrayList<>();
        for (final Map.Entry<PaperCard, Integer> e : from) {
            final CardRules rules = e.getKey().getRules();
            if (df.isLegalCommander(rules) && !rules.getType().hasSubtype("Background")
                    && candidates.stream().noneMatch(c -> c.getName().equals(e.getKey().getName()))) {
                candidates.add(e.getKey());
            }
        }
        if (candidates.size() == 1) {
            final PaperCard only = candidates.get(0);
            from.remove(only, 1);
            deck.getOrCreate(DeckSection.Commander).add(only, 1);
            return only.getName();
        }
        final List<ImportFix> fixes = new ArrayList<>();
        for (final PaperCard c : candidates.subList(0, Math.min(MOST_COMMANDER_CHOICES, candidates.size()))) {
            fixes.add(new ImportFix(ImportFixKind.commander, c.getName(), c.getName()));
        }
        fixes.add(new ImportFix(ImportFixKind.other, Localizer.getInstance().getMessage("lblWebImportOther"), null));
        problems.add(new ImportProblem(-1, Localizer.getInstance().getMessage("lblWebImportNoCommander"), candidates.isEmpty()
                ? Localizer.getInstance().getMessage("lblWebImportNoLeader")
                : Localizer.getInstance().getMessage("lblWebImportNoCommanderSection"), fixes));
        return null;
    }
}
