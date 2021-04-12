package forge.gamemodes.tournament;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import forge.deck.Deck;
import forge.deck.DeckType;
import forge.deck.DeckgenUtil;
import forge.model.FModel;
import forge.util.MyRandom;

public class TournamentUtil {
    public static TournamentData createQuickTournament(final Deck userDeck, final int numOpponents, final List<DeckType> allowedDeckTypes) {
        TournamentData tournament = new TournamentData();
        setDefaultTournamentName(tournament, TournamentIO.PREFIX_QUICK);
        FModel.setTournamentData(tournament);

        // Generate tournament decks
        Deck deck;
        final List<String> eventNames = new ArrayList<>();
        final List<Deck> decks = new ArrayList<>();

        for (int i = 0; i < numOpponents; i++) {
            int randType = (int)Math.floor(MyRandom.getRandom().nextDouble() * allowedDeckTypes.size());
            switch (allowedDeckTypes.get(randType)) {
                case COLOR_DECK:
                    deck = DeckgenUtil.getRandomColorDeck(true);
                    eventNames.add("Random colors deck");
                    break;
                case STANDARD_COLOR_DECK:
                    deck = DeckgenUtil.getRandomColorDeck(FModel.getFormats().getStandard().getFilterPrinted(),true);
                    break;
                case STANDARD_CARDGEN_DECK:
                    deck = DeckgenUtil.buildCardGenDeck(FModel.getFormats().getStandard(),true);
                    break;
                case PIONEER_CARDGEN_DECK:
                    deck = DeckgenUtil.buildCardGenDeck(FModel.getFormats().getPioneer(),true);
                    break;
                case HISTORIC_CARDGEN_DECK:
                    deck = DeckgenUtil.buildCardGenDeck(FModel.getFormats().getHistoric(),true);
                    break;
                case MODERN_CARDGEN_DECK:
                    deck = DeckgenUtil.buildCardGenDeck(FModel.getFormats().getModern(),true);
                    break;
                case LEGACY_CARDGEN_DECK:
                    deck = DeckgenUtil.buildLDACArchetypeDeck(FModel.getFormats().get("Legacy"),true);
                    break;
                case VINTAGE_CARDGEN_DECK:
                    deck = DeckgenUtil.buildLDACArchetypeDeck(FModel.getFormats().get("Vintage"),true);
                    break;
                case MODERN_COLOR_DECK:
                    deck = DeckgenUtil.getRandomColorDeck(FModel.getFormats().getModern().getFilterPrinted(),true);
                    break;
                case CUSTOM_DECK:
                    deck = DeckgenUtil.getRandomCustomDeck();
                    eventNames.add(deck.getName());
                    break;
                case PRECONSTRUCTED_DECK:
                    deck = DeckgenUtil.getRandomPreconDeck();
                    eventNames.add(deck.getName());
                    break;
                case QUEST_OPPONENT_DECK:
                    deck = DeckgenUtil.getRandomQuestDeck();
                    eventNames.add(deck.getName());
                    break;
                case THEME_DECK:
                    deck = DeckgenUtil.getRandomThemeDeck();
                    eventNames.add(deck.getName());
                    break;
                default:
                    continue;
            }
            decks.add(deck);
        }

        tournament.setDecks(decks);
        tournament.setEventNames(eventNames);
        tournament.setUserDeck(userDeck);

        // Reset all variable fields to 0, stamps and saves automatically.
        tournament.reset();
        return tournament;
    }

    public static void setDefaultTournamentName(TournamentData tournament, String prefix) {
        final File[] arrFiles = TournamentIO.getTournamentFilesUnlocked(prefix);
        final Set<String> setNames = new HashSet<>();
        for (File f : arrFiles) {
            setNames.add(f.getName());
        }

        int num = 1;
        while (setNames.contains(prefix + num + TournamentIO.SUFFIX_DATA)) {
            num++;
        }
        tournament.setName(prefix + num);
    }
}
