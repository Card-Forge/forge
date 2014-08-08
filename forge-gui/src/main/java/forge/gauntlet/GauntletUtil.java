package forge.gauntlet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import forge.deck.Deck;
import forge.deck.DeckType;
import forge.deck.DeckgenUtil;
import forge.model.FModel;

public class GauntletUtil {
    public static GauntletData createQuickGauntlet(final Deck userDeck, final int numOpponents, final List<DeckType> allowedDeckTypes) {
        final File[] arrFiles = GauntletIO.getGauntletFilesQuick();
        final Set<String> setNames = new HashSet<String>();
        for (File f : arrFiles) { setNames.add(f.getName()); }

        int num = 1;
        while (setNames.contains(GauntletIO.PREFIX_QUICK + num + GauntletIO.SUFFIX_DATA)) { num++; }
        FModel.getGauntletData().setName(GauntletIO.PREFIX_QUICK + num);

        // Generate gauntlet decks
        final List<String> lstEventNames = new ArrayList<String>();
        final List<Deck> lstGauntletDecks = new ArrayList<Deck>();
        Deck tempDeck;

        for (int i = 0; i < numOpponents; i++) {
            int randType = (int)Math.floor(Math.random() * allowedDeckTypes.size());
            switch (allowedDeckTypes.get(randType)) {
            case COLOR_DECK:
                tempDeck = DeckgenUtil.getRandomColorDeck(true);
                lstEventNames.add("Random colors deck");
                break;
            case CUSTOM_DECK:
                tempDeck = DeckgenUtil.getRandomCustomDeck();
                lstEventNames.add(tempDeck.getName());
                break;
            case PRECONSTRUCTED_DECK:
                tempDeck = DeckgenUtil.getRandomPreconDeck();
                lstEventNames.add(tempDeck.getName());
                break;
            case QUEST_OPPONENT_DECK:
                tempDeck = DeckgenUtil.getRandomQuestDeck();
                lstEventNames.add(tempDeck.getName());
                break;
            case THEME_DECK:
                tempDeck = DeckgenUtil.getRandomThemeDeck();
                lstEventNames.add(tempDeck.getName());
                break;
            default:
                continue;
            }
            lstGauntletDecks.add(tempDeck);
        }

        final GauntletData gd = FModel.getGauntletData();
        gd.setDecks(lstGauntletDecks);
        gd.setEventNames(lstEventNames);
        gd.setUserDeck(userDeck);

        // Reset all variable fields to 0, stamps and saves automatically.
        gd.reset();
        return gd;
    }
}
