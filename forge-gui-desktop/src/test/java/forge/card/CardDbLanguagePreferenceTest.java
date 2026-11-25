package forge.card;

import forge.CardStorageReader;
import forge.ImageKeys;
import forge.StaticData;
import forge.item.PaperCard;
import forge.util.Localizer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class CardDbLanguagePreferenceTest {
    private StaticData staticData;
    private CardDb cardDb;

    @BeforeClass
    public void setUp() {
        Localizer.getInstance().initialize("en-US", "../forge-gui/res/languages");
        ImageKeys.initializeDirs("target/test-images/cards", Collections.emptyMap(),
                "target/test-images/tokens", "target/test-images/icons", "target/test-images/boosters",
                "target/test-images/fatpacks", "target/test-images/boosterboxes",
                "target/test-images/precons", "target/test-images/tournamentpacks");
        CardStorageReader reader = new CardStorageReader("../forge-gui/res/cardsfolder", null, true);
        staticData = new StaticData(reader, null,
                "../forge-gui/res/editions", "target/nonexistent-custom-editions",
                "../forge-gui/res/blockdata", "Latest Art All Editions", true, false);
        cardDb = staticData.getCommonCards();
    }

    @BeforeMethod
    public void resetLanguagePreference() {
        cardDb.setPreferredCardLanguage("en-US");
    }

    @Test
    public void automaticPrintingPrefersSelectedLanguage() {
        String cardName = "Earthquake";

        staticData.attemptToLoadCard(cardName);
        cardDb.setPreferredCardLanguage("ja-JP");

        PaperCard automaticCard = cardDb.getCard(cardName);
        assertNotNull(automaticCard);
        assertEquals(staticData.getCardEdition(automaticCard.getEdition()).getCardsLangCode(), "ja");

        PaperCard uniqueCard = cardDb.getUniqueByNameNoAlt(cardName);
        assertNotNull(uniqueCard);
        assertEquals(staticData.getCardEdition(uniqueCard.getEdition()).getCardsLangCode(), "ja");
    }

    @Test
    public void automaticPrintingFallsBackToEnglish() {
        String cardName = "Earthquake";

        staticData.attemptToLoadCard(cardName);
        cardDb.setPreferredCardLanguage("fr-FR");

        PaperCard automaticCard = cardDb.getCard(cardName);
        assertNotNull(automaticCard);
        assertEquals(staticData.getCardEdition(automaticCard.getEdition()).getCardsLangCode(), "en");

        PaperCard uniqueCard = cardDb.getUniqueByNameNoAlt(cardName);
        assertNotNull(uniqueCard);
        assertEquals(staticData.getCardEdition(uniqueCard.getEdition()).getCardsLangCode(), "en");
    }

    @Test
    public void automaticPrintingFallsBackToAnyAvailableLanguage() {
        String cardName = "Earthquake";
        String japaneseEdition = "PMDA";

        staticData.attemptToLoadCard(cardName);
        cardDb.setPreferredCardLanguage("fr-FR");

        PaperCard fallbackCard = cardDb.getCardFromEditions(cardName, cardDb.getCardArtPreference(), 1,
                card -> japaneseEdition.equals(card.getEdition()));
        assertNotNull(fallbackCard);
        assertEquals(fallbackCard.getEdition(), japaneseEdition);
        assertEquals(staticData.getCardEdition(fallbackCard.getEdition()).getCardsLangCode(), "ja");
    }

    @Test
    public void explicitEditionIsPreserved() {
        String cardName = "Earthquake";
        String japaneseEdition = "PMDA";

        staticData.attemptToLoadCard(cardName);
        cardDb.setPreferredCardLanguage("fr-FR");

        PaperCard explicitJapaneseCard = cardDb.getCard(cardName, japaneseEdition);
        assertNotNull(explicitJapaneseCard);
        assertEquals(explicitJapaneseCard.getEdition(), japaneseEdition);
        assertEquals(staticData.getCardEdition(explicitJapaneseCard.getEdition()).getCardsLangCode(), "ja");
    }
}
