package forge.deck;

import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.ForgeCardMockTestCase;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.deck.DeckRecognizer.Token;
import forge.deck.DeckRecognizer.TokenType;
import forge.model.FModel;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeckRecognizerTest extends ForgeCardMockTestCase {

    private Set<String> mtgUniqueCardNames;
    private Set<String> mtgUniqueSetCodes;
    private Set<String> mtgUniqueCollectorNumbers;

    private void initMaps(){
        StaticData magicDb = FModel.getMagicDb();
        mtgUniqueCardNames = new HashSet<>();
        mtgUniqueSetCodes = new HashSet<>();
        mtgUniqueCollectorNumbers = new HashSet<>();
        Collection<PaperCard> fullCardDb = magicDb.getCommonCards().getAllCards();
        for (PaperCard card : fullCardDb) {
            this.mtgUniqueCardNames.add(card.getName());
            CardEdition e = magicDb.getCardEdition(card.getEdition());
            if (e != null) {
                this.mtgUniqueSetCodes.add(e.getCode());
                this.mtgUniqueSetCodes.add(e.getScryfallCode());
            }
            String cn = card.getCollectorNumber();
            if (!cn.equals(IPaperCard.NO_COLLECTOR_NUMBER))
                this.mtgUniqueCollectorNumbers.add(cn);
        }
    }

    /* ======================================
     * TEST SINGLE (Card and Non-Card) Parts
     * - CardName
     * - Card Amount
     * - Set Code
     * - Collector Number
     * - Deck Name
     * - Card Type
     * - Deck Section
     * ======================================
     */

    @Test void testMatchingCardName(){
        if (mtgUniqueCardNames == null)
            this.initMaps();
        Pattern cardNamePattern = Pattern.compile(DeckRecognizer.REX_CARD_NAME);
        for (String cardName : this.mtgUniqueCardNames){
            Matcher cardNameMatcher = cardNamePattern.matcher(cardName);
            assertTrue(cardNameMatcher.matches(), "Fail on " + cardName);
            String matchedCardName = cardNameMatcher.group(DeckRecognizer.REGRP_CARD);
            assertEquals(matchedCardName, cardName, "Fail on " + cardName);
        }
    }

    @Test void testMatchingFoilCardName(){
        String foilCardName = "Counter spell+";
        Pattern cardNamePattern = Pattern.compile(DeckRecognizer.REX_CARD_NAME);
        Matcher cardNameMatcher = cardNamePattern.matcher(foilCardName);
        assertTrue(cardNameMatcher.matches(), "Fail on " + foilCardName);
        String matchedCardName = cardNameMatcher.group(DeckRecognizer.REGRP_CARD);
        assertEquals(matchedCardName, foilCardName, "Fail on " + foilCardName);
    }


    @Test void testMatchingSetCodes(){
        if (mtgUniqueCardNames == null)
            this.initMaps();
        Pattern setCodePattern = Pattern.compile(DeckRecognizer.REX_SET_CODE);
        for (String setCode : this.mtgUniqueSetCodes){
            Matcher setCodeMatcher = setCodePattern.matcher(setCode);
            assertTrue(setCodeMatcher.matches(), "Fail on " + setCode);
            String matchedSetCode = setCodeMatcher.group(DeckRecognizer.REGRP_SET);
            assertEquals(matchedSetCode, setCode, "Fail on " + setCode);
        }
    }

    @Test void testMatchingCollectorNumber(){
        if (mtgUniqueCardNames == null)
            this.initMaps();
        Pattern collNumberPattern = Pattern.compile(DeckRecognizer.REX_COLL_NUMBER);
        for (String collectorNumber : this.mtgUniqueCollectorNumbers){
            Matcher collNumberMatcher = collNumberPattern.matcher(collectorNumber);
            assertTrue(collNumberMatcher.matches());
            String matchedCollNr = collNumberMatcher.group(DeckRecognizer.REGRP_COLLNR);
            assertEquals(matchedCollNr, collectorNumber, "Fail on " + collectorNumber);
        }
    }

    @Test void testCardQuantityRequest(){
        Pattern cardCountPattern = Pattern.compile(DeckRecognizer.REX_CARD_COUNT);
        String[] correctAmountRequests = new String[] {"0", "2", "12", "4", "8x", "12x"};
        String[] inCorrectAmountRequests = new String[] {"-2", "-23", "NO", "133"};

        for (String correctReq : correctAmountRequests) {
            Matcher matcher = cardCountPattern.matcher(correctReq);
            assertTrue(matcher.matches());
            String expectedRequestAmount = correctReq;
            if (correctReq.endsWith("x"))
                expectedRequestAmount = correctReq.substring(0, correctReq.length()-1);

            assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), expectedRequestAmount);
            int expectedAmount = Integer.parseInt(expectedRequestAmount);
            assertEquals(Integer.parseInt(matcher.group(DeckRecognizer.REGRP_CARDNO)), expectedAmount);
        }

        for (String incorrectReq : inCorrectAmountRequests) {
            Matcher matcher = cardCountPattern.matcher(incorrectReq);
            assertFalse(matcher.matches());
        }
    }

    @Test void testMatchDeckName(){
        Pattern deckNamePattern = DeckRecognizer.DECK_NAME_PATTERN;

        String matchingDeckName = "Deck: Red Green Aggro";
        Matcher deckNameMatcher = deckNamePattern.matcher(matchingDeckName);
        assertTrue(deckNameMatcher.matches());
        assertTrue(DeckRecognizer.isDeckName(matchingDeckName));
        assertEquals(deckNameMatcher.group(DeckRecognizer.REGRP_DECKNAME), "Red Green Aggro");
        assertEquals(DeckRecognizer.deckNameMatch(matchingDeckName), "Red Green Aggro");

        matchingDeckName = "Name: Red Green Aggro";
        deckNameMatcher = deckNamePattern.matcher(matchingDeckName);
        assertTrue(deckNameMatcher.matches());
        assertTrue(DeckRecognizer.isDeckName(matchingDeckName));
        assertEquals(deckNameMatcher.group(DeckRecognizer.REGRP_DECKNAME), "Red Green Aggro");
        assertEquals(DeckRecognizer.deckNameMatch(matchingDeckName), "Red Green Aggro");

        matchingDeckName = "Name:Red Green Aggro";
        deckNameMatcher = deckNamePattern.matcher(matchingDeckName);
        assertTrue(deckNameMatcher.matches());
        assertTrue(DeckRecognizer.isDeckName(matchingDeckName));
        assertEquals(deckNameMatcher.group(DeckRecognizer.REGRP_DECKNAME), "Red Green Aggro");
        assertEquals(DeckRecognizer.deckNameMatch(matchingDeckName), "Red Green Aggro");

        matchingDeckName = "Name:       Red Green Aggro";
        deckNameMatcher = deckNamePattern.matcher(matchingDeckName);
        assertTrue(deckNameMatcher.matches());
        assertTrue(DeckRecognizer.isDeckName(matchingDeckName));
        assertEquals(deckNameMatcher.group(DeckRecognizer.REGRP_DECKNAME), "Red Green Aggro");
        assertEquals(DeckRecognizer.deckNameMatch(matchingDeckName), "Red Green Aggro");

        matchingDeckName = "Deck:Red Green Aggro";
        deckNameMatcher = deckNamePattern.matcher(matchingDeckName);
        assertTrue(deckNameMatcher.matches());
        assertTrue(DeckRecognizer.isDeckName(matchingDeckName));
        assertEquals(deckNameMatcher.group(DeckRecognizer.REGRP_DECKNAME), "Red Green Aggro");
        assertEquals(DeckRecognizer.deckNameMatch(matchingDeckName), "Red Green Aggro");

        // Case Insensitive
        matchingDeckName = "deck: Red Green Aggro";
        deckNameMatcher = deckNamePattern.matcher(matchingDeckName);
        assertTrue(deckNameMatcher.matches());
        assertTrue(DeckRecognizer.isDeckName(matchingDeckName));
        assertEquals(deckNameMatcher.group(DeckRecognizer.REGRP_DECKNAME), "Red Green Aggro");
        assertEquals(DeckRecognizer.deckNameMatch(matchingDeckName), "Red Green Aggro");

        // Forge deck format
        matchingDeckName = "Name=Sliver Overlord (Commander)";
        deckNameMatcher = deckNamePattern.matcher(matchingDeckName);
        assertTrue(deckNameMatcher.matches());
        assertTrue(DeckRecognizer.isDeckName(matchingDeckName));
        assertEquals(deckNameMatcher.group(DeckRecognizer.REGRP_DECKNAME), "Sliver Overlord (Commander)");
        assertEquals(DeckRecognizer.deckNameMatch(matchingDeckName), "Sliver Overlord (Commander)");

        // Failing Cases
        matchingDeckName = ":Red Green Aggro";
        deckNameMatcher = deckNamePattern.matcher(matchingDeckName);
        assertFalse(deckNameMatcher.matches());
        assertFalse(DeckRecognizer.isDeckName(matchingDeckName));
        assertEquals(DeckRecognizer.deckNameMatch(matchingDeckName), "");

        matchingDeckName = "Red Green Aggro";
        deckNameMatcher = deckNamePattern.matcher(matchingDeckName);
        assertFalse(deckNameMatcher.matches());
        assertFalse(DeckRecognizer.isDeckName(matchingDeckName));
        assertEquals(DeckRecognizer.deckNameMatch(matchingDeckName), "");

        matchingDeckName = "Name-Red Green Aggro";
        deckNameMatcher = deckNamePattern.matcher(matchingDeckName);
        assertFalse(deckNameMatcher.matches());
        assertFalse(DeckRecognizer.isDeckName(matchingDeckName));
        assertEquals(DeckRecognizer.deckNameMatch(matchingDeckName), "");

        matchingDeckName = "Deck.Red Green Aggro";
        deckNameMatcher = deckNamePattern.matcher(matchingDeckName);
        assertFalse(deckNameMatcher.matches());
        assertFalse(DeckRecognizer.isDeckName(matchingDeckName));
        assertEquals(DeckRecognizer.deckNameMatch(matchingDeckName), "");

        matchingDeckName = ":Red Green Aggro";
        deckNameMatcher = deckNamePattern.matcher(matchingDeckName);
        assertFalse(deckNameMatcher.matches());
        assertFalse(DeckRecognizer.isDeckName(matchingDeckName));
        assertEquals(DeckRecognizer.deckNameMatch(matchingDeckName), "");
    }

    @Test void testMatchDeckSectionNames(){
        String[] dckSections = new String[] {"Main", "main", "Mainboard",
                "Sideboard", "Side", "Schemes", "Avatar", "avatar", "Commander", "Conspiracy", "card", "Planes"};
        for (String section : dckSections)
            assertTrue(DeckRecognizer.isDeckSectionName(section), "Unrecognised Deck Section: " + section);

        assertFalse(DeckRecognizer.isDeckSectionName("Avatar of Hope"));
        assertFalse(DeckRecognizer.isDeckSectionName("Conspiracy Theory"));
        assertFalse(DeckRecognizer.isDeckSectionName("Planeswalker's Mischief"));
        assertFalse(DeckRecognizer.isDeckSectionName("Demon of Dark Schemes"));

        String[] deckSectionEntriesFoundInMDExportFromTappedOut = new String[]{
                "## Sideboard (15)", "## Mainboard (86)"};
        for (String entry: deckSectionEntriesFoundInMDExportFromTappedOut)
            assertTrue(DeckRecognizer.isDeckSectionName(entry), "Fail on "+entry);

        String[] deckSectionEntriesFoundInDCKFormat = new String[] {"[Main]", "[Sideboard]"};
        for (String entry: deckSectionEntriesFoundInDCKFormat)
            assertTrue(DeckRecognizer.isDeckSectionName(entry), "Fail on "+entry);
    }

    @Test void testMatchCardTypes(){
        String[] cardTypes = new String[] {"Spell", "instants", "Sorceries", "Sorcery",
                "Artifact", "creatures", "land"};
        for (String cardType : cardTypes)
            assertTrue(DeckRecognizer.isCardType(cardType), "Fail on "+cardType);

        String[] cardTypesEntriesFoundInMDExportFromTappedOut = new String[]{
                "### Instant (14)", "### Sorcery (9)", "### Artifact (14)",
                "### Creature (2)", "### Land (21)", };
        for (String entry: cardTypesEntriesFoundInMDExportFromTappedOut)
            assertTrue(DeckRecognizer.isCardType(entry), "Fail on "+entry);

        String[] cardTypesInDecFormat = new String[] {
                "//Lands", "//Artifacts", "//Enchantments",
                "//Instants", "//Sorceries", "//Planeswalkers",
                "//Creatures"};
        for (String entry : cardTypesInDecFormat)
            assertTrue(DeckRecognizer.isCardType(entry), "Fail on " + entry);
    }

    @Test void testOnlyContainingCardTypeWontMatchCardTypeToken(){
        String[] nonCardTypes = new String[] {"Spell collection", "instants list",
                "creatures elves", "land list"};
        for (String nonCardTypeTokens : nonCardTypes)
            assertFalse(DeckRecognizer.isCardType(nonCardTypeTokens), "Fail on "+nonCardTypeTokens);
    }

    @Test void testRarityTypeTokenMatch(){
        String[] rarityTokens = new String[] {"Common", "uncommon", "rare", "mythic", "mythic rare", "land"};
        for (String line : rarityTokens)
            assertTrue(DeckRecognizer.isCardRarity(line), "Fail on "+line);

        String[] nonRarityTokens = new String[] {"Common cards", "uncommon cards", "mythics", "rares", "lands"};
        for (String line : nonRarityTokens)
            assertFalse(DeckRecognizer.isCardRarity(line), "Fail on "+line);
    }

    @Test void testCMCTokenMatch(){
        String[] cmcTokens = new String[] {"CC0", "CMC2", "CMC11", "cc3"};
        for (String line : cmcTokens)
            assertTrue(DeckRecognizer.isCardCMC(line), "Fail on "+line);

        String[] nonCMCtokens = new String[] {"cc", "CMC", "cc322", "cmc111"};
        for (String line : nonCMCtokens)
            assertFalse(DeckRecognizer.isCardCMC(line), "Fail on "+line);
    }

    @Test void testManaTokenMatch(){
        String[] cmcTokens = new String[] {"Blue", "red", "White", "// Black", "       //Colorless----", "(green)"};
        for (String line : cmcTokens)
            assertTrue(DeckRecognizer.isManaToken(line), "Fail on " + line);

        String[] nonCMCtokens = new String[] {"blues", "red more words", "mainboard"};
        for (String line : nonCMCtokens)
            assertFalse(DeckRecognizer.isManaToken(line), "Fail on "+line);
    }

    /*=============================
    * TEST RECOGNISE NON-CARD LINES
    * =============================
    */
    @Test void testMatchNonCardLine(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        // Test Token Types
        Token t = recognizer.recogniseNonCardToken("//Lands");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_TYPE);
        assertEquals(t.getText(), "Lands");
        assertEquals(t.getNumber(), 0);

        // Test Token Types
        t = recognizer.recogniseNonCardToken("//Land");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_RARITY);
        assertEquals(t.getText(), "Land");
        assertEquals(t.getNumber(), 0);

        t = recognizer.recogniseNonCardToken("[Main]");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.DECK_SECTION_NAME);
        assertEquals(t.getText(), "Main");
        assertEquals(t.getNumber(), 0);

        t = recognizer.recogniseNonCardToken("## Mainboard (75)");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.DECK_SECTION_NAME);
        assertEquals(t.getText(), "Main");
        assertEquals(t.getNumber(), 0);

        t = recognizer.recogniseNonCardToken("### Artifact (3)");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_TYPE);
        assertEquals(t.getText(), "Artifact");
        assertEquals(t.getNumber(), 0);

        t = recognizer.recogniseNonCardToken("Enchantments");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_TYPE);
        assertEquals(t.getText(), "Enchantments");
        assertEquals(t.getNumber(), 0);

        t = recognizer.recogniseNonCardToken("//Name: Artifacts from DeckStats.net");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.DECK_NAME);
        assertEquals(t.getText(), "Artifacts from DeckStats");
        assertEquals(t.getNumber(), 0);

        t = recognizer.recogniseNonCardToken("Name: OLDSCHOOL 93-94 Red Green Aggro by Zombies with JetPack");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.DECK_NAME);
        assertEquals(t.getText(), "OLDSCHOOL 93-94 Red Green Aggro by Zombies with JetPack");
        assertEquals(t.getNumber(), 0);

        t = recognizer.recogniseNonCardToken("CMC0");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_CMC);
        assertEquals(t.getText(), "CMC0");
        assertEquals(t.getNumber(), 0);

        t = recognizer.recogniseNonCardToken("CC1");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_CMC);
        assertEquals(t.getText(), "CC1");
        assertEquals(t.getNumber(), 0);

        t = recognizer.recogniseNonCardToken("//Common");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_RARITY);
        assertEquals(t.getText(), "Common");
        assertEquals(t.getNumber(), 0);

        t = recognizer.recogniseNonCardToken("(mythic rare)");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_RARITY);
        assertEquals(t.getText(), "mythic rare");
        assertEquals(t.getNumber(), 0);

        t = recognizer.recogniseNonCardToken("//Blue");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.MANA_COLOUR);
        assertEquals(t.getText(), "Blue");
        assertEquals(t.getNumber(), 0);

        t = recognizer.recogniseNonCardToken("(Colorless)");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.MANA_COLOUR);
        assertEquals(t.getText(), "Colorless");
        assertEquals(t.getNumber(), 0);

        t = recognizer.recogniseNonCardToken("//Planes");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.DECK_SECTION_NAME);
        assertEquals(t.getText(), "Planes");
        assertEquals(t.getNumber(), 0);
    }

    /*=============================
     * TEST RECOGNISE CARD LINES
     * =============================
     */

    // === Card-Set Request
    @Test void testValidMatchCardSetLine(){
        String validRequest = "1 Power Sink TMP";
        Matcher matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "1");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "Power Sink TMP";  // no count
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "Power Sink tmp";  // set cde in lower case
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "tmp");

        validRequest = "10 Power Sink TMP";  // double digits
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "10");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        // x multiplier symbol in card count
        validRequest = "12x Power Sink TMP";
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "12");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        // -- Set code with delimiters
        validRequest = "Power Sink|TMP";  // pipe delimiter
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "Power Sink (TMP)";  // MTGArena-like
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink "); // requires trim
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "Power Sink|TMP";  // pipe delimiter
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "Power Sink [TMP]"; // .Dec-like delimiter
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink "); // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "Power Sink|TMP";  // pipe delimiter
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "Power Sink {TMP}";  // Alternative braces delimiter
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink ");  // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        // -- also valid = delimiters can also be partial

        validRequest = "Power Sink (TMP";  // pipe delimiter
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink ");  // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "Power Sink(TMP)";  // No space
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "Power Sink|TMP";  // pipe delimiter
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "Power Sink [TMP)";  // Mixed
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink ");  // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "Power Sink TMP]";  // last bracket to be stripped, but using space from card name
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
    }

    @Test void testInvalidMatchCardSetLine(){
        // == Invalid Cases for this REGEX
        // Remeber: this rex *always* expects a Set Code!

        String invalidRequest = "Power Sink ";  // mind last space
        Matcher matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(invalidRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power");
        assertNotNull(matcher.group(DeckRecognizer.REGRP_SET));
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "Sink");

        invalidRequest = "22 Power Sink";  // mind last space
        matcher = DeckRecognizer.CARD_SET_PATTERN.matcher(invalidRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "22");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power");
        assertNotNull(matcher.group(DeckRecognizer.REGRP_SET));
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "Sink");
    }

    // === Set-Card Request
    @Test void testValidMatchSetCardLine(){
        String validRequest = "1 TMP Power Sink";
        Matcher matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "1");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "TMP Power Sink";  // no count
        matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "tmp Power Sink";  // set code in lowercase
        matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "tmp");

        validRequest = "10 TMP Power Sink";  // double digits
        matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "10");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        // x multiplier symbol in card count
        validRequest = "12x TMP Power Sink";
        matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "12");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        // -- separators
        validRequest = "TMP|Power Sink";  // pipe-after set code
        matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "(TMP)Power Sink";  // parenthesis
        matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "[TMP]Power Sink";  // brackets
        matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "{TMP}Power Sink";  // braces
        matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "TMP    Power Sink";  // lots of spaces
        matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "(TMP|Power Sink";  // mixed
        matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "(TMP]Power Sink";  // mixed 2
        matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "TMP]Power Sink";  // partial
        matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "TMP]    Power Sink";  // partial with spaces
        matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        validRequest = "(TMP Power Sink";  // only left-handside
        matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
    }

    @Test void testInvalidMatchSetCardLine(){
        // == Invalid Cases for this REGEX
        // Remeber: this rex *always* expects a Set Code!

        String invalidRequest = "Power Sink";  // mind last space
        Matcher matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(invalidRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Sink");
        assertNotNull(matcher.group(DeckRecognizer.REGRP_SET));
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "Power");

        invalidRequest = "22 Power Sink";  // mind last space
        matcher = DeckRecognizer.SET_CARD_PATTERN.matcher(invalidRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "22");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Sink");
        assertNotNull(matcher.group(DeckRecognizer.REGRP_SET));
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "Power");
    }

    //=== Card-Set-CollectorNumber request
    @Test void testMatchFullCardSetRequest(){
        String validRequest = "1 Power Sink TMP 78";
        Matcher matcher = DeckRecognizer.CARD_SET_COLLNO_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "1");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");

        validRequest = "1 Power Sink|TMP 78";
        matcher = DeckRecognizer.CARD_SET_COLLNO_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "1");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");

        validRequest = "1 Power Sink (TMP) 78";  // MTG Arena alike
        matcher = DeckRecognizer.CARD_SET_COLLNO_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "1");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink ");  // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");

        validRequest = "Power Sink (TMP) 78";  // MTG Arena alike
        matcher = DeckRecognizer.CARD_SET_COLLNO_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink ");  // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");
    }

    @Test void testInvalidMatchFullCardSetRequest(){
        // NOTE: this will be matcher by another pattern
        String invalidRequest = "1 Power Sink TMP";  // missing collector number
        Matcher matcher = DeckRecognizer.CARD_SET_COLLNO_PATTERN.matcher(invalidRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "1");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "TMP");

        // Note: this will be matched by another pattern
        invalidRequest = "1 TMP Power Sink";
        matcher = DeckRecognizer.CARD_SET_COLLNO_PATTERN.matcher(invalidRequest);
        assertFalse(matcher.matches());
    }

    // === Set-Card-CollectorNumber Request
    @Test void testMatchFullSetCardRequest(){
        String validRequest = "1 TMP Power Sink 78";
        Matcher matcher = DeckRecognizer.SET_CARD_COLLNO_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "1");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");

        validRequest = "4x TMP Power Sink 78";
        matcher = DeckRecognizer.SET_CARD_COLLNO_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "4");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");

        validRequest = "TMP Power Sink 78";
        matcher = DeckRecognizer.SET_CARD_COLLNO_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");

        validRequest = "(TMP) Power Sink 78";
        matcher = DeckRecognizer.SET_CARD_COLLNO_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");

        validRequest = "TMP|Power Sink 78";
        matcher = DeckRecognizer.SET_CARD_COLLNO_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");

        validRequest = "[TMP] Power Sink 78";
        matcher = DeckRecognizer.SET_CARD_COLLNO_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");

        validRequest = "TMP| Power Sink 78";  // extra space
        matcher = DeckRecognizer.SET_CARD_COLLNO_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");

        validRequest = "tmp Power Sink 78";  // set code lowercase
        matcher = DeckRecognizer.SET_CARD_COLLNO_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "tmp");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");

        validRequest = "(TMP} Power Sink 78";  // mixed delimiters - still valid :D
        matcher = DeckRecognizer.SET_CARD_COLLNO_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");
    }

    @Test void testInvalidMatchFullSetCardRequest(){
        // NOTE: this will be matcher by another pattern
        String invalidRequest = "1 Power Sink TMP";  // missing collector number
        Matcher matcher = DeckRecognizer.SET_CARD_COLLNO_PATTERN.matcher(invalidRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "1");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "Power");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "TMP");

        // Note: this will be matched by another pattern
        invalidRequest = "1 TMP Power Sink";
        matcher = DeckRecognizer.SET_CARD_COLLNO_PATTERN.matcher(invalidRequest);
        assertFalse(matcher.matches());
    }

    @Test void testCrossRexForDifferentLineRequests(){
        String cardRequest = "4x Power Sink TMP 78";
        assertTrue(DeckRecognizer.CARD_SET_COLLNO_PATTERN.matcher(cardRequest).matches());
        assertTrue(DeckRecognizer.SET_CARD_COLLNO_PATTERN.matcher(cardRequest).matches());
        assertTrue(DeckRecognizer.CARD_SET_PATTERN.matcher(cardRequest).matches());
        assertTrue(DeckRecognizer.SET_CARD_PATTERN.matcher(cardRequest).matches());

        cardRequest = "4x TMP Power Sink 78";
        assertTrue(DeckRecognizer.CARD_SET_COLLNO_PATTERN.matcher(cardRequest).matches());
        assertTrue(DeckRecognizer.SET_CARD_COLLNO_PATTERN.matcher(cardRequest).matches());
        assertTrue(DeckRecognizer.CARD_SET_PATTERN.matcher(cardRequest).matches());
        assertTrue(DeckRecognizer.SET_CARD_PATTERN.matcher(cardRequest).matches());

        cardRequest = "4x Power Sink TMP";
        assertTrue(DeckRecognizer.CARD_SET_COLLNO_PATTERN.matcher(cardRequest).matches());
        assertTrue(DeckRecognizer.SET_CARD_COLLNO_PATTERN.matcher(cardRequest).matches());
        assertTrue(DeckRecognizer.CARD_SET_PATTERN.matcher(cardRequest).matches());
        assertTrue(DeckRecognizer.SET_CARD_PATTERN.matcher(cardRequest).matches());

        cardRequest = "4x TMP Power Sink";
        // ONLY CASES IN WHICH THIS WILL FAIL
        assertFalse(DeckRecognizer.CARD_SET_COLLNO_PATTERN.matcher(cardRequest).matches());
        assertFalse(DeckRecognizer.SET_CARD_COLLNO_PATTERN.matcher(cardRequest).matches());

        assertTrue(DeckRecognizer.CARD_SET_PATTERN.matcher(cardRequest).matches());
        assertTrue(DeckRecognizer.SET_CARD_PATTERN.matcher(cardRequest).matches());
    }

    // === Card Only Request

    @Test void testMatchCardOnlyRequest(){
        String validRequest = "4x Power Sink";
        Matcher matcher = DeckRecognizer.CARD_ONLY_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "4");

        validRequest = "Power Sink";
        matcher = DeckRecognizer.CARD_ONLY_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));

        String invalidRequest = "";
        matcher = DeckRecognizer.CARD_ONLY_PATTERN.matcher(invalidRequest);
        assertFalse(matcher.matches());

        invalidRequest = "TMP";  // set code (there is no way for this to not match)
        matcher = DeckRecognizer.CARD_ONLY_PATTERN.matcher(invalidRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "TMP");
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));

        invalidRequest = "78";  // collector number (there is no way for this to not match)
        matcher = DeckRecognizer.CARD_ONLY_PATTERN.matcher(invalidRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "78");
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
    }

    @Test void testMatchFoilCardRequest(){
        // card-set-collnr
        String foilRequest = "4x Power Sink+ (TMP) 78";
        Pattern target = DeckRecognizer.CARD_SET_COLLNO_PATTERN;
        Matcher matcher = target.matcher(foilRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "4");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink+ ");  // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");

        // Set-card-collnr
        foilRequest = "4x (TMP) Power Sink+ 78";
        target = DeckRecognizer.SET_CARD_COLLNO_PATTERN;
        matcher = target.matcher(foilRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "4");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink+");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");

        // set-card
        foilRequest = "4x (TMP) Power Sink+";
        target = DeckRecognizer.SET_CARD_PATTERN;
        matcher = target.matcher(foilRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "4");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink+");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        // card-set
        foilRequest = "4x Power Sink+ (TMP)";
        target = DeckRecognizer.CARD_SET_PATTERN;
        matcher = target.matcher(foilRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "4");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink+ "); // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");

        // card-only
        foilRequest = "4x Power Sink+";
        target = DeckRecognizer.CARD_ONLY_PATTERN;
        matcher = target.matcher(foilRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "4");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink+");
    }

    @Test void testMatchFoilCardRequestMTGGoldfishFormat(){
        // card-set-collnr
        String foilRequest = "4 Aspect of Hydra [BNG] (F)";
        Pattern target = DeckRecognizer.CARD_SET_COLLNO_PATTERN;
        Matcher matcher = target.matcher(foilRequest);
        assertFalse(matcher.matches());

        foilRequest = "4 Aspect of Hydra [BNG] 117 (F)";
        target = DeckRecognizer.CARD_SET_COLLNO_PATTERN;
        matcher = target.matcher(foilRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "4");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Aspect of Hydra ");  // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "BNG");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "117");
        assertNotNull(matcher.group(DeckRecognizer.REGRP_FOIL_GFISH));

        // Set-card-collnr
        foilRequest = "4 [BNG] Aspect of Hydra (F)";
        target = DeckRecognizer.SET_CARD_COLLNO_PATTERN;
        matcher = target.matcher(foilRequest);
        assertFalse(matcher.matches());

        foilRequest = "4 [BNG] Aspect of Hydra 117 (F)";
        target = DeckRecognizer.SET_CARD_COLLNO_PATTERN;
        matcher = target.matcher(foilRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "4");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Aspect of Hydra");
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "BNG");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "117");
        assertNotNull(matcher.group(DeckRecognizer.REGRP_FOIL_GFISH));

        // set-card
        foilRequest = "4 [BNG] Aspect of Hydra (F)";
        target = DeckRecognizer.SET_CARD_PATTERN;
        matcher = target.matcher(foilRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "4");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Aspect of Hydra ");  // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "BNG");
        assertEquals(matcher.group(DeckRecognizer.REGRP_FOIL_GFISH), "(F)");

        // card-set
        foilRequest = "4 Aspect of Hydra [BNG] (F)";
        target = DeckRecognizer.CARD_SET_PATTERN;
        matcher = target.matcher(foilRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "4");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Aspect of Hydra ");  // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "BNG");
        assertEquals(matcher.group(DeckRecognizer.REGRP_FOIL_GFISH), "(F)");

        // card-only
        foilRequest = "4 Aspect of Hydra (F)";
        target = DeckRecognizer.CARD_ONLY_PATTERN;
        matcher = target.matcher(foilRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "4");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Aspect of Hydra ");  // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_FOIL_GFISH), "(F)");
    }

    @Test void testRecogniseCardToken(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        String lineRequest = "4x Power Sink+ (TMP) 78";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        PaperCard tokenCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertTrue(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertEquals(tokenCard.getCollectorNumber(), "78");

        lineRequest = "4x Power Sink TMP 78";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertEquals(tokenCard.getCollectorNumber(), "78");

        lineRequest = "4x TMP Power Sink 78";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertEquals(tokenCard.getCollectorNumber(), "78");

        lineRequest = "4x TMP Power Sink";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertEquals(tokenCard.getCollectorNumber(), "78");

        lineRequest = "4x Power Sink TMP";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertEquals(tokenCard.getCollectorNumber(), "78");

        lineRequest = "Power Sink TMP";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 1);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertEquals(tokenCard.getCollectorNumber(), "78");

        lineRequest = "[TMP] Power Sink";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 1);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertEquals(tokenCard.getCollectorNumber(), "78");

        // Relax Set Preference
        assertEquals(db.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        lineRequest = "4x Power Sink";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "VMA");

        lineRequest = "Power Sink";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 1);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "VMA");
    }

    @Test void testRecognisingCardFromSetUsingAlternateCode(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        String lineRequest = "4x Power Sink+ TE 78";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        PaperCard tokenCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertTrue(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertEquals(tokenCard.getCollectorNumber(), "78");
    }

    @Test void testSingleWordCardNameMatchesCorrectly(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        String lineRequest = "2x Counterspell ICE";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        PaperCard tokenCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 2);
        assertEquals(tokenCard.getName(), "Counterspell");
        assertEquals(tokenCard.getEdition(), "ICE");

        // Remove Set code
        assertEquals(db.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        lineRequest = "2x Counterspell";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 2);
        assertEquals(tokenCard.getName(), "Counterspell");
        assertEquals(tokenCard.getEdition(), "MH2");

    }

    @Test void testPassingInArtIndexRatherThanCollectorNumber(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        String lineRequest = "20x Mountain MIR 3";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        PaperCard tokenCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 20);
        assertEquals(tokenCard.getName(), "Mountain");
        assertEquals(tokenCard.getEdition(), "MIR");
        assertEquals(tokenCard.getArtIndex(), 3);
        assertEquals(tokenCard.getCollectorNumber(), "345");
    }

    @Test void testCollectorNumberIsNotConfusedAsArtIndexInstead(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        String lineRequest = "2x Auspicious Ancestor MIR 3";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        PaperCard tokenCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 2);
        assertEquals(tokenCard.getName(), "Auspicious Ancestor");
        assertEquals(tokenCard.getEdition(), "MIR");
        assertEquals(tokenCard.getArtIndex(), 1);
        assertEquals(tokenCard.getCollectorNumber(), "3");
    }

    @Test void testCardRequestWithWrongCollectorNumberStillReturnsTheCardFromSetIfAny(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        String requestLine = "3 Jayemdae Tome (LEB) 231";  // actually found in TappedOut Deck Export
        // NOTE: Expected Coll Nr should be 255
        Token cardToken = recognizer.recogniseCardToken(requestLine);
        assertNotNull(cardToken);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getNumber(), 3);
        PaperCard card = cardToken.getCard();
        assertEquals(card.getName(), "Jayemdae Tome");
        assertEquals(card.getEdition(), "LEB");
        assertEquals(card.getCollectorNumber(), "255");

        // No Match - Unknown card
        requestLine = "3 Jayemdae Tome (TMP)";  // actually found in TappedOut Deck Export
        // NOTE: Expected Coll Nr should be 255
        cardToken = recognizer.recogniseCardToken(requestLine);
        assertNotNull(cardToken);
        assertNull(cardToken.getCard());
        assertEquals(cardToken.getType(), TokenType.UNKNOWN_CARD_REQUEST);
    }

    @Test void testRequestingCardFromTheWrongSetReturnsUnknownCard(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        String lineRequest = "2x Counterspell FEM";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.UNKNOWN_CARD_REQUEST);
        assertNull(cardToken.getCard());
    }

    @Test void testRequestingCardFromNonExistingSetReturnsUnknownCard(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        String lineRequest = "2x Counterspell BOU";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.UNKNOWN_CARD_REQUEST);
        assertNull(cardToken.getCard());
    }

    @Test void testRequestingCardWithRestrictionsOnSetsFromGameFormat(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);
        // Setting Fantasy Constructed Game Format: Urza's Block Format
        List<String> allowedSets = Arrays.asList("USG", "ULG", "UDS", "PUDS", "PULG", "PUSG");
        recognizer.setGameFormatConstraint(allowedSets);

        String lineRequest = "2x Counterspell ICE";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.ILLEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());

        lineRequest = "2x Counterspell";  // It does not exist any Counterspell in Urza's block
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.ILLEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
    }

    @Test void testRequestingCardWithRestrictionsOnDeckFormat(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        String lineRequest = "Ancestral Recall";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        PaperCard ancestralCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 1);
        assertEquals(ancestralCard.getName(), "Ancestral Recall");
        assertEquals(db.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        assertEquals(ancestralCard.getEdition(), "VMA");

        recognizer.setDeckFormatConstraint(DeckFormat.TinyLeaders);
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.ILLEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
    }

    @Test void testRequestingCardWithReleaseDateConstraints(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);
        recognizer.setDateConstraint(2002, 1);
        assertEquals(db.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String lineRequest = "Ancestral Recall";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        PaperCard ancestralCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 1);
        assertEquals(ancestralCard.getName(), "Ancestral Recall");
        assertEquals(db.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        assertEquals(ancestralCard.getEdition(), "2ED");

        // Counterspell editions
        lineRequest = "Counterspell";
        recognizer.setDateConstraint(1999, 10);
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        PaperCard counterSpellCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 1);
        assertEquals(counterSpellCard.getName(), "Counterspell");
        assertEquals(counterSpellCard.getEdition(), "MMQ");
    }

    @Test void testInvalidCardRequestWhenReleaseDateConstraintsAreUp(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        assertEquals(db.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        // First run without constraint to check that it's a valid request
        String lineRequest = "Counterspell|MH2";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        PaperCard counterSpellCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 1);
        assertEquals(counterSpellCard.getName(), "Counterspell");
        assertEquals(counterSpellCard.getEdition(), "MH2");

        recognizer.setDateConstraint(1999, 10);
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.INVALID_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
    }

    @Test void testCardMatchWithDateANDGameFormatConstraints(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        // Baseline - no constraints
        assertEquals(db.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        String lineRequest = "2x Lightning Dragon";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getNumber(), 2);
        PaperCard tc = cardToken.getCard();
        assertEquals(tc.getName(), "Lightning Dragon");
        assertEquals(tc.getEdition(), "VMA");

        recognizer.setDateConstraint(2000, 0);  // Jan 2000
        // Setting Fantasy Constructed Game Format: Urza's Block Format (no promo)
        List<String> allowedSets = Arrays.asList("USG", "ULG", "UDS");
        recognizer.setGameFormatConstraint(allowedSets);

        lineRequest = "2x Lightning Dragon|USG";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getNumber(), 2);
        tc = cardToken.getCard();
        assertEquals(tc.getName(), "Lightning Dragon");
        assertEquals(tc.getEdition(), "USG");

        // Relaxing Constraint on Set
        lineRequest = "2x Lightning Dragon";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertEquals(cardToken.getNumber(), 2);
        assertNotNull(cardToken.getCard());
        tc = cardToken.getCard();
        assertEquals(tc.getName(), "Lightning Dragon");
        assertEquals(tc.getEdition(), "USG");  // the latest available within set requested

        // Now setting a tighter date constraint
        recognizer.setDateConstraint(1998, 0);  // Jan 1998
        lineRequest = "2x Lightning Dragon|USG";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.INVALID_CARD_REQUEST);
        assertEquals(cardToken.getNumber(), 2);
        assertNotNull(cardToken.getCard());

        lineRequest = "2x Lightning Dragon";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.INVALID_CARD_REQUEST);
        assertEquals(cardToken.getNumber(), 2);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Lightning Dragon (USG)");

        // Now relaxing date constraint but removing USG from allowed sets
        // VMA release date: 2014-06-16
        recognizer.setDateConstraint(2015, 0);  // This will match VMA
        recognizer.setGameFormatConstraint(Arrays.asList("ULG", "UDS"));

        lineRequest = "2x Lightning Dragon|USG";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.ILLEGAL_CARD_REQUEST);
        assertEquals(cardToken.getNumber(), 2);
        assertNotNull(cardToken.getCard());

        lineRequest = "2x Lightning Dragon";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.ILLEGAL_CARD_REQUEST);
        assertEquals(cardToken.getNumber(), 2);
        assertNotNull(cardToken.getCard());

        // Now relaxing date constraint but removing USG from allowed sets
        // VMA release date: 2014-06-16
        recognizer.setDateConstraint(2015, 0);  // This will match VMA
        recognizer.setGameFormatConstraint(Arrays.asList("VMA", "ULG", "UDS"));
        lineRequest = "2x Lightning Dragon";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertEquals(cardToken.getNumber(), 2);
        assertNotNull(cardToken.getCard());
        tc = cardToken.getCard();
        assertEquals(tc.getName(), "Lightning Dragon");
        assertEquals(tc.getEdition(), "VMA");
    }

    @Test void testCardMatchWithDateANDdeckFormatConstraints(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        // Baseline - no constraints
        assertEquals(db.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String lineRequest = "Flash";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getNumber(), 1);
        PaperCard tc = cardToken.getCard();
        assertEquals(tc.getName(), "Flash");
        assertEquals(tc.getEdition(), "A25");

        recognizer.setDateConstraint(2012, 0);  // Jan 2012
        recognizer.setDeckFormatConstraint(DeckFormat.TinyLeaders);

        lineRequest = "Flash";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.ILLEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Flash (6ED)");

        lineRequest = "2x Cancel";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertEquals(cardToken.getNumber(), 2);
        assertNotNull(cardToken.getCard());
        tc = cardToken.getCard();
        assertEquals(tc.getName(), "Cancel");
        assertEquals(tc.getEdition(), "M12");  // the latest within date constraint

        lineRequest = "2x Cancel|M21";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.INVALID_CARD_REQUEST);
        assertEquals(cardToken.getNumber(), 2);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Cancel (M21)");
    }

    @Test void testCardMatchWithGameANDdeckFormatConstraints(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        // Baseline - no constraints
        assertEquals(db.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String lineRequest = "Flash";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getNumber(), 1);
        PaperCard tc = cardToken.getCard();
        assertEquals(tc.getName(), "Flash");
        assertEquals(tc.getEdition(), "A25");

        recognizer.setGameFormatConstraint(Arrays.asList("MIR", "VIS", "WTH"));
        recognizer.setDeckFormatConstraint(DeckFormat.TinyLeaders);

        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.ILLEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Flash (MIR)");

        lineRequest = "2x Femeref Knight";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertEquals(cardToken.getNumber(), 2);
        assertNotNull(cardToken.getCard());
        tc = cardToken.getCard();
        assertEquals(tc.getName(), "Femeref Knight");
        assertEquals(tc.getEdition(), "MIR");

        lineRequest = "2x Incinerate";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertEquals(cardToken.getNumber(), 2);
        assertNotNull(cardToken.getCard());
        tc = cardToken.getCard();
        assertEquals(tc.getName(), "Incinerate");
        assertEquals(tc.getEdition(), "MIR");

        lineRequest = "Noble Elephant";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.ILLEGAL_CARD_REQUEST);  // violating Deck format
        assertEquals(cardToken.getNumber(), 1);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Noble Elephant (MIR)");

        lineRequest = "Incinerate|ICE";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.ILLEGAL_CARD_REQUEST);  // violating Game format
        assertEquals(cardToken.getNumber(), 1);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Incinerate (ICE)");
    }

    @Test void testCardMatchWitDateANDgameANDdeckFormatConstraints(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        // Baseline - no constraints
        assertEquals(db.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String lineRequest = "Flash";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getNumber(), 1);
        PaperCard tc = cardToken.getCard();
        assertEquals(tc.getName(), "Flash");
        assertEquals(tc.getEdition(), "A25");

        recognizer.setGameFormatConstraint(Arrays.asList("MIR", "VIS", "WTH"));
        recognizer.setDeckFormatConstraint(DeckFormat.TinyLeaders);
        recognizer.setDateConstraint(1999, 2);  // March '99

        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.ILLEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Flash (MIR)");

        lineRequest = "Ardent Militia";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.ILLEGAL_CARD_REQUEST);  // illegal in deck format
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Ardent Militia (WTH)");  // within set constraints

        lineRequest = "Buried Alive|UMA";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.ILLEGAL_CARD_REQUEST);  // illegal in game format
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Buried Alive (UMA)");  // within set constraints

        lineRequest = "Buried Alive";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);  // illegal in deck format
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Buried Alive");
        assertEquals(cardToken.getCard().getEdition(), "WTH");  // within set constraints

        recognizer.setDateConstraint(1997, 2);  // March '97 - before WTH
        lineRequest = "Buried Alive";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.INVALID_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Buried Alive (WTH)");
    }

    /*==================================
     * TEST RECOGNISE CARD EXTRA FORMATS
     * =================================
     */

    // === MTG Goldfish
    @Test void testFoilRequestInMTGGoldfishExportFormat(){
        String mtgGoldfishRequest = "18 Forest <254> [THB]";
        Pattern target = DeckRecognizer.CARD_COLLNO_SET_PATTERN;
        Matcher matcher = target.matcher(mtgGoldfishRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "18");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Forest");  // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "THB");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "254");
        assertNull(matcher.group(DeckRecognizer.REGRP_FOIL_GFISH));

        mtgGoldfishRequest = "18 Forest <254> [THB] (F)";
        matcher = target.matcher(mtgGoldfishRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "18");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Forest");  // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "THB");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "254");
        assertNotNull(matcher.group(DeckRecognizer.REGRP_FOIL_GFISH));

        mtgGoldfishRequest = "18 Forest [THB]";
        matcher = target.matcher(mtgGoldfishRequest);
        assertFalse(matcher.matches());

        mtgGoldfishRequest = "18 [THB] Forest";
        matcher = target.matcher(mtgGoldfishRequest);
        assertFalse(matcher.matches());

        mtgGoldfishRequest = "18 Forest [THB] (F)";
        matcher = target.matcher(mtgGoldfishRequest);
        assertFalse(matcher.matches());

        mtgGoldfishRequest = "18 [THB] Forest (F)";
        matcher = target.matcher(mtgGoldfishRequest);
        assertFalse(matcher.matches());

        mtgGoldfishRequest = "18 Forest 254 [THB] (F)";
        matcher = target.matcher(mtgGoldfishRequest);
        assertFalse(matcher.matches());

        mtgGoldfishRequest = "18 Forest 254 [THB]";
        matcher = target.matcher(mtgGoldfishRequest);
        assertFalse(matcher.matches());

        mtgGoldfishRequest = "18 [THB] Forest 254";
        matcher = target.matcher(mtgGoldfishRequest);
        assertFalse(matcher.matches());
    }

    @Test void testCardRecognisedMTGGoldfishFormat(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);
        assertEquals(db.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String lineRequest = "4 Aspect of Hydra [BNG] (F)";
        Token cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        PaperCard aspectOfHydraCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 4);
        assertEquals(aspectOfHydraCard.getName(), "Aspect of Hydra");
        assertEquals(aspectOfHydraCard.getEdition(), "BNG");
        assertTrue(aspectOfHydraCard.isFoil());

        lineRequest = "18 Forest <254> [THB] (F)";
        cardToken = recognizer.recogniseCardToken(lineRequest);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNotNull(cardToken.getCard());
        PaperCard forestCard = cardToken.getCard();
        assertEquals(cardToken.getNumber(), 18);
        assertEquals(forestCard.getName(), "Forest");
        assertEquals(forestCard.getEdition(), "THB");
        assertTrue(forestCard.isFoil());
    }

    // === TappedOut Markdown Format
    @Test void testPurgeLinksInLineRequests(){
        String line = "* 1 [Ancestral Recall](http://tappedout.nethttp://tappedout.net/mtg-card/ancestral-recall/)";
        String expected = "* 1 [Ancestral Recall]";
        assertEquals(DeckRecognizer.purgeAllLinks(line), expected);

        line = "1 [Ancestral Recall](http://tappedout.nethttp://tappedout.net/mtg-card/ancestral-recall/)";
        expected = "1 [Ancestral Recall]";
        assertEquals(DeckRecognizer.purgeAllLinks(line), expected);
    }

    @Test void testCardNameEntryInMarkDownExportFromTappedOut(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);
        assertEquals(db.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String line = "* 1 [Ancestral Recall](http://tappedout.nethttp://tappedout.net/mtg-card/ancestral-recall/)";

        Token token = recognizer.recognizeLine(line);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertEquals(token.getNumber(), 1);
        assertNotNull(token.getCard());
        PaperCard ancestralRecallCard = token.getCard();
        assertEquals(ancestralRecallCard.getName(), "Ancestral Recall");
        assertEquals(ancestralRecallCard.getEdition(), "VMA");
    }

    // === XMage Format
    @Test void testMatchCardRequestXMageFormat(){
        String xmageFormatRequest = "1 [LRW:51] Amoeboid Changeling";
        Pattern target = DeckRecognizer.SET_COLLNO_CARD_XMAGE_PATTERN;
        Matcher matcher = target.matcher(xmageFormatRequest);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARDNO), "1");
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Amoeboid Changeling");  // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "LRW");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "51");
        assertNull(matcher.group(DeckRecognizer.REGRP_FOIL_GFISH));

        // Test that this line matches only with this pattern

        target = DeckRecognizer.CARD_SET_PATTERN;
        matcher = target.matcher(xmageFormatRequest);
        assertFalse(matcher.matches());

        target = DeckRecognizer.SET_CARD_PATTERN;
        matcher = target.matcher(xmageFormatRequest);
        assertFalse(matcher.matches());

        target = DeckRecognizer.CARD_SET_COLLNO_PATTERN;
        matcher = target.matcher(xmageFormatRequest);
        assertFalse(matcher.matches());

        target = DeckRecognizer.SET_CARD_COLLNO_PATTERN;
        matcher = target.matcher(xmageFormatRequest);
        assertFalse(matcher.matches());

        target = DeckRecognizer.CARD_COLLNO_SET_PATTERN;
        matcher = target.matcher(xmageFormatRequest);
        assertFalse(matcher.matches());

        target = DeckRecognizer.CARD_ONLY_PATTERN;
        matcher = target.matcher(xmageFormatRequest);
        assertFalse(matcher.matches());
    }

    @Test void testRecognizeCardTokenInXMageFormatRequest(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        String xmageFormatRequest = "1 [LRW:51] Amoeboid Changeling";
        Token xmageCardToken = recognizer.recogniseCardToken(xmageFormatRequest);
        assertNotNull(xmageCardToken);
        assertEquals(xmageCardToken.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertEquals(xmageCardToken.getNumber(), 1);
        assertNotNull(xmageCardToken.getCard());
        PaperCard acCard = xmageCardToken.getCard();
        assertEquals(acCard.getName(), "Amoeboid Changeling");
        assertEquals(acCard.getEdition(), "LRW");
        assertEquals(acCard.getCollectorNumber(), "51");
    }

    /*====================================
     * TEST RECOGNISE LINES (MIXED inputs)
     * ===================================
     */
    @Test void testRecognizeLines(){
        StaticData magicDb = FModel.getMagicDb();
        CardDb db = magicDb.getCommonCards();
        CardDb altDb = magicDb.getVariantCards();
        DeckRecognizer recognizer = new DeckRecognizer(db, altDb);

        String lineRequest = "// MainBoard";
        Token token = recognizer.recognizeLine(lineRequest);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.DECK_SECTION_NAME);
        assertEquals(token.getText(), "Main");

        lineRequest = "## Sideboard (15)";
        token = recognizer.recognizeLine(lineRequest);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.DECK_SECTION_NAME);
        assertEquals(token.getText(), "Sideboard");

        lineRequest = "Normal Text";
        token = recognizer.recognizeLine(lineRequest);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.UNKNOWN_TEXT);
        assertEquals(token.getText(), "Normal Text");

        lineRequest = "//Creatures";
        token = recognizer.recognizeLine(lineRequest);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.CARD_TYPE);
        assertEquals(token.getText(), "Creatures");

        lineRequest = "//Lands";
        token = recognizer.recognizeLine(lineRequest);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.CARD_TYPE);
        assertEquals(token.getText(), "Lands");

        lineRequest = "//Land";
        token = recognizer.recognizeLine(lineRequest);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.CARD_RARITY);
        assertEquals(token.getText(), "Land");

        lineRequest = "//Creatures with text";
        token = recognizer.recognizeLine(lineRequest);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.COMMENT);
        assertEquals(token.getText(), "//Creatures with text");

        lineRequest = "SB:Ancestral Recall";
        token = recognizer.recognizeLine(lineRequest);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.COMMENT);
        assertEquals(token.getText(), "SB:Ancestral Recall");

        lineRequest = "Ancestral Recall";
        token = recognizer.recognizeLine(lineRequest);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNull(token.getText());
        assertNotNull(token.getCard());

        lineRequest = "* 4 [Counterspell](http://tappedout.nethttp://tappedout.net/mtg-card/counterspell/)";
        token = recognizer.recognizeLine(lineRequest);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.LEGAL_CARD_REQUEST);
        assertNull(token.getText());
        assertNotNull(token.getCard());
        assertEquals(token.getNumber(), 4);

        lineRequest = "### Instant (14)";
        token = recognizer.recognizeLine(lineRequest);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.CARD_TYPE);
        assertEquals(token.getText(), "Instant");

        lineRequest = "### General line as comment";
        token = recognizer.recognizeLine(lineRequest);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.COMMENT);
        assertEquals(token.getText(), "### General line as comment");
    }
}
