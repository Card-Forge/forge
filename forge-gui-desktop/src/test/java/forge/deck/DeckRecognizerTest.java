package forge.deck;

import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.ForgeCardMockTestCase;
import forge.card.MagicColor;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.deck.DeckRecognizer.Token;
import forge.deck.DeckRecognizer.TokenType;
import forge.model.FModel;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
     * - Mana Colour
     * ======================================
     */

    /*==================================
     * Rex Parsing and Matching: CARD DB
     * ================================= */
    @Test void testMatchAllCardNamesInForgeDB(){
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

    @Test void testMatchAllSetCodesAndAlternateCodesInForgeDB(){
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

    @Test void testMatchAllPossibleCollectorNumbersInForgeDB(){
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

    @Test void testMatchingFoilCardName(){
        String foilCardName = "Counter spell+";
        Pattern cardNamePattern = Pattern.compile(DeckRecognizer.REX_CARD_NAME);
        Matcher cardNameMatcher = cardNamePattern.matcher(foilCardName);
        assertTrue(cardNameMatcher.matches(), "Fail on " + foilCardName);
        String matchedCardName = cardNameMatcher.group(DeckRecognizer.REGRP_CARD);
        assertEquals(matchedCardName, foilCardName, "Fail on " + foilCardName);
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

    @Test void testDeckNameAsInNetDecksWithSymbols(){
        String deckName = "Name = [Standard] #02 - Dimir Rogues";
        Pattern deckNamePattern = DeckRecognizer.DECK_NAME_PATTERN;
        Matcher deckNameMatcher = deckNamePattern.matcher(deckName);
        assertTrue(deckNameMatcher.matches());
        assertTrue(DeckRecognizer.isDeckName(deckName));
        assertEquals(deckNameMatcher.group(DeckRecognizer.REGRP_DECKNAME), "[Standard] #02 - Dimir Rogues");
        assertEquals(DeckRecognizer.deckNameMatch(deckName), "[Standard] #02 - Dimir Rogues");
    }

    @Test void testMatchDeckSectionNames(){
        String[] dckSections = new String[] {"Main", "main", "Mainboard", "Sideboard", "Side", "Schemes", "Avatar",
                "avatar", "Commander", "Conspiracy", "card", "Planes", "Dungeon"};
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

        String[] deckSectionEntriesFoundInDCKFormat = new String[] {"[Main]", "[Sideboard]", "[Dungeon]"};
        for (String entry: deckSectionEntriesFoundInDCKFormat)
            assertTrue(DeckRecognizer.isDeckSectionName(entry), "Fail on "+entry);
    }

    @Test void testSBshortAsPlaceholderForSideboard(){
        String dckSec = "SB:";
        assertTrue(DeckRecognizer.isDeckSectionName(dckSec));

        DeckRecognizer recognizer = new DeckRecognizer();
        Token token = recognizer.recogniseNonCardToken(dckSec);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.DECK_SECTION_NAME);
        assertEquals(token.getText(), "Sideboard");
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
        String[] rarityTokens = new String[] {"Common", "uncommon", "rare", "mythic", "mythic rare", "land", "special"};
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

    @Test void testManaSymbolsMatches(){
        Pattern manaSymbolPattern = DeckRecognizer.MANA_PATTERN;

        List<MagicColor.Color> colours = Arrays.asList(MagicColor.Color.COLORLESS, MagicColor.Color.BLACK,
                MagicColor.Color.BLUE, MagicColor.Color.GREEN, MagicColor.Color.RED, MagicColor.Color.GREEN);

        for (MagicColor.Color color : colours){
            String matchingManaSymbol = color.getSymbol();
            Matcher manaSymbolMatcher = manaSymbolPattern.matcher(matchingManaSymbol);
            assertTrue(manaSymbolMatcher.matches(), "Failed on : " + matchingManaSymbol);
        }
        // Lowercase
        for (MagicColor.Color color : colours){
            String matchingManaSymbol = color.getSymbol().toLowerCase();
            Matcher manaSymbolMatcher = manaSymbolPattern.matcher(matchingManaSymbol);
            assertTrue(manaSymbolMatcher.matches(), "Failed on : " + matchingManaSymbol);
        }

        // No Brackets - SO expected to fail matching
        for (MagicColor.Color color : colours){
            String matchingManaSymbol = color.getSymbol().toLowerCase().substring(1, color.getSymbol().length());
            Matcher manaSymbolMatcher = manaSymbolPattern.matcher(matchingManaSymbol);
            assertFalse(manaSymbolMatcher.matches(), "Failed on : " + matchingManaSymbol);
        }

        // Test Multi-Colour
        Matcher manaSymbolMatcher = manaSymbolPattern.matcher("{m}");
        assertTrue(manaSymbolMatcher.matches());

        manaSymbolMatcher = manaSymbolPattern.matcher("{M}");
        assertTrue(manaSymbolMatcher.matches());

        manaSymbolMatcher = manaSymbolPattern.matcher("m");
        assertFalse(manaSymbolMatcher.matches());

        manaSymbolMatcher = manaSymbolPattern.matcher("ubwm");
        assertFalse(manaSymbolMatcher.matches());
    }

    @Test void testManaTokenMatch(){
        DeckRecognizer recognizer = new DeckRecognizer();
        String[] cmcTokens = new String[] {"Blue", "red", "White", "// Black",
                "       //Colorless----", "(green)",
                "// Multicolor", "// MultiColour"};

        String cname = ForgeCardMockTestCase.MOCKED_LOCALISED_STRING;
        String[] expectedTokenText = new String[] {
                String.format("%s {U}", cname), String.format("%s {R}", cname),
                String.format("%s {W}", cname), String.format("%s {B}", cname),
                String.format("%s {C}", cname), String.format("%s {G}", cname),
                String.format("%s {W}{U}{B}{R}{G}", cname), String.format("%s {W}{U}{B}{R}{G}", cname)
        };
        for (int i = 0; i < cmcTokens.length; i++) {
            String line = cmcTokens[i];
            assertTrue(DeckRecognizer.isManaToken(line), "Fail on " + line);
            Token manaToken = recognizer.recogniseNonCardToken(line);
            assertNotNull(manaToken);
            assertEquals(manaToken.getText(), expectedTokenText[i]);
        }

        String[] nonCMCtokens = new String[] {"blues", "red more words", "mainboard"};
        for (String line : nonCMCtokens)
            assertFalse(DeckRecognizer.isManaToken(line), "Fail on "+line);
    }

    @Test void testManaTokensBiColors(){
        DeckRecognizer recognizer = new DeckRecognizer();
        String[] cmcTokens = new String[] {
                "Blue White", "red-black", "White green", "// Black Blue", "(green|red)"};
        String[] manaTokens = new String[] {
                "{U} {W}", "{r}-{b}", "{W} {g}", "// {B} {U}", "({g}|{r})"};
        String[] mixedSymbols = new String[] {
                "{u} White", "{R}-black", "White {g}", "// {b} Blue", "(green|{r})"
        };
        String cname = ForgeCardMockTestCase.MOCKED_LOCALISED_STRING;
        String[] expectedTokenText = new String[] {
                String.format("%s/%s %s", cname, cname, "{WU}"),
                String.format("%s/%s %s", cname, cname, "{BR}"),
                String.format("%s/%s %s", cname, cname, "{GW}"),
                String.format("%s/%s %s", cname, cname, "{UB}"),
                String.format("%s/%s %s", cname, cname, "{RG}")
        };

        for (int i = 0; i < cmcTokens.length; i++) {
            String line = cmcTokens[i];
            assertTrue(DeckRecognizer.isManaToken(line), "Fail on " + line);
            Token manaToken = recognizer.recogniseNonCardToken(line);
            assertNotNull(manaToken);
            assertEquals(manaToken.getText(), expectedTokenText[i]);
            //Symbol
            String symbol = manaTokens[i];
            assertTrue(DeckRecognizer.isManaToken(symbol), "Fail on " + symbol);
            Token manaSymbolToken = recognizer.recogniseNonCardToken(symbol);
            assertNotNull(manaSymbolToken);
            assertEquals(manaSymbolToken.getText(), expectedTokenText[i]);
            // Mixed
            String mixedSymbol = mixedSymbols[i];
            assertTrue(DeckRecognizer.isManaToken(mixedSymbol), "Fail on " + mixedSymbol);
            Token mixedManaSymbolToken = recognizer.recogniseNonCardToken(mixedSymbol);
            assertNotNull(mixedManaSymbolToken);
            assertEquals(mixedManaSymbolToken.getText(), expectedTokenText[i]);
        }
    }

    @Test void testTokenBiColorSymbols(){
        DeckRecognizer recognizer = new DeckRecognizer();
        String[] manaSymbols = new String[] {"{WU}", "{UB}", "{BR}", "{GW}", "{RG}",
                "{WB}", "{UR}", "{BG}", "{RW}", "{GU}"};

        String cname = ForgeCardMockTestCase.MOCKED_LOCALISED_STRING;
        String[] expectedTokenText = new String[] {
                String.format("%s/%s %s", cname, cname, "{WU}"),
                String.format("%s/%s %s", cname, cname, "{UB}"),
                String.format("%s/%s %s", cname, cname, "{BR}"),
                String.format("%s/%s %s", cname, cname, "{GW}"),
                String.format("%s/%s %s", cname, cname, "{RG}"),
                String.format("%s/%s %s", cname, cname, "{WB}"),
                String.format("%s/%s %s", cname, cname, "{UR}"),
                String.format("%s/%s %s", cname, cname, "{BG}"),
                String.format("%s/%s %s", cname, cname, "{RW}"),
                String.format("%s/%s %s", cname, cname, "{GU}")
        };

        for (int i = 0; i < manaSymbols.length; i++) {
            String manaSymbol = manaSymbols[i];
            assertTrue(DeckRecognizer.isManaToken(manaSymbol), "Fail on " + manaSymbol);
            Token manaToken = recognizer.recogniseNonCardToken(manaSymbol);
            assertNotNull(manaToken);
            assertEquals(manaToken.getText(), expectedTokenText[i]);
        }
    }

    @Test void testManaTokensRepeatedAreIgnored(){
        DeckRecognizer recognizer = new DeckRecognizer();
        String[] cmcTokens = new String[] {"Blue Blue", "red-red", "White white",
                "// black BLACK", "(Green|grEEn)", };
        String[] expectedTokenText = new String[] {"{U}", "{R}", "{W}", "{B}", "{G}"};
        for (int i = 0; i < cmcTokens.length; i++) {
            String line = cmcTokens[i];
            assertTrue(DeckRecognizer.isManaToken(line), "Fail on " + line);
            Token manaToken = recognizer.recogniseNonCardToken(line);
            assertNotNull(manaToken);
            assertTrue(manaToken.getText().endsWith(expectedTokenText[i]));
        }

        String[] manaTokens = new String[] {"{u} {u}", "{R}-{R}", "{w} {W}",
                "// {b} {B}", "({G}|{g})", };
        String[] expectedManaSymbols = new String[] {"{U}", "{R}", "{W}", "{B}", "{G}"};
        for (int i = 0; i < manaTokens.length; i++) {
            String line = manaTokens[i];
            assertTrue(DeckRecognizer.isManaToken(line), "Fail on " + line);
            Token manaToken = recognizer.recogniseNonCardToken(line);
            assertNotNull(manaToken);
            assertTrue(manaToken.getText().endsWith(expectedManaSymbols[i]));
            assertFalse(manaToken.getText().endsWith(
                    String.format("%s %s", expectedManaSymbols[i], expectedManaSymbols[i])));
            assertFalse(manaToken.getText().endsWith(
                    String.format("%s%s", expectedManaSymbols[i], expectedManaSymbols[i])));
            assertFalse(manaToken.getText().endsWith(
                    String.format("%s/%s", expectedManaSymbols[i], expectedManaSymbols[i])));
        }
    }

    @Test void testMultiColourOrColourlessManaTokensWillBeHandledSeparately(){
        DeckRecognizer recognizer = new DeckRecognizer();
        String[] cmcTokens = new String[] {"Blue Colourless", "Red Multicolour", "Colorless White",
                "// Multicolour ", "(green|Colourless)"};
        String cname = ForgeCardMockTestCase.MOCKED_LOCALISED_STRING;
        String[] expectedTokenText = new String[] {
                String.format("%s {U} // %s {C}", cname, cname),
                String.format("%s {R} // %s {W}{U}{B}{R}{G}", cname, cname),
                String.format("%s {C} // %s {W}", cname, cname),
                String.format("%s {W}{U}{B}{R}{G}", cname),
                String.format("%s {G} // %s {C}", cname, cname)};
        for (int i = 0; i < cmcTokens.length; i++) {
            String line = cmcTokens[i];
            assertTrue(DeckRecognizer.isManaToken(line), "Fail on " + line);
            Token manaToken = recognizer.recogniseNonCardToken(line);
            assertNotNull(manaToken);
            assertEquals(manaToken.getText(), expectedTokenText[i]);
        }
    }

    @Test void testCornerCasesWithSpecialMulticolourAndColorlessTokens(){
        DeckRecognizer recognizer = new DeckRecognizer();
        // Test repeated
        String[] cmcTokens = new String[] {"Colorless Colourless", "Multicolor Multicolour",
                "Colorless colourless"};

        String cname = ForgeCardMockTestCase.MOCKED_LOCALISED_STRING;
        String[] expectedTokenText = new String[] {
                String.format("%s {C}", cname),
                String.format("%s {W}{U}{B}{R}{G}", cname),
                String.format("%s {C}", cname)};

        for (int i = 0; i < cmcTokens.length; i++) {
            String line = cmcTokens[i];
            assertTrue(DeckRecognizer.isManaToken(line), "Fail on " + line);
            Token manaToken = recognizer.recogniseNonCardToken(line);
            assertNotNull(manaToken);
            assertEquals(manaToken.getText(), expectedTokenText[i]);
        }

        // Test symbols
    }

    /*=============================
    * TEST RECOGNISE NON-CARD LINES
    * ============================= */
    @Test void testMatchNonCardLine(){
        DeckRecognizer recognizer = new DeckRecognizer();

        // Test Token Types
        Token t = recognizer.recogniseNonCardToken("//Lands");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_TYPE);
        assertEquals(t.getText(), "Lands");
        assertEquals(t.getQuantity(), 0);

        // Test Token Types
        t = recognizer.recogniseNonCardToken("//Land");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_RARITY);
        assertEquals(t.getText(), "Land");
        assertEquals(t.getQuantity(), 0);

        t = recognizer.recogniseNonCardToken("[Main]");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.DECK_SECTION_NAME);
        assertEquals(t.getText(), "Main");
        assertEquals(t.getQuantity(), 0);

        t = recognizer.recogniseNonCardToken("## Mainboard (75)");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.DECK_SECTION_NAME);
        assertEquals(t.getText(), "Main");
        assertEquals(t.getQuantity(), 0);

        t = recognizer.recogniseNonCardToken("### Artifact (3)");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_TYPE);
        assertEquals(t.getText(), "Artifact");
        assertEquals(t.getQuantity(), 0);

        t = recognizer.recogniseNonCardToken("Enchantments");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_TYPE);
        assertEquals(t.getText(), "Enchantments");
        assertEquals(t.getQuantity(), 0);

        t = recognizer.recogniseNonCardToken("//Name: Artifacts from DeckStats.net");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.DECK_NAME);
        assertEquals(t.getText(), "Artifacts from DeckStats");
        assertEquals(t.getQuantity(), 0);

        t = recognizer.recogniseNonCardToken("Name: OLDSCHOOL 93-94 Red Green Aggro by Zombies with JetPack");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.DECK_NAME);
        assertEquals(t.getText(), "OLDSCHOOL 93-94 Red Green Aggro by Zombies with JetPack");
        assertEquals(t.getQuantity(), 0);

        t = recognizer.recogniseNonCardToken("CMC0");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_CMC);
        assertEquals(t.getText(), "CMC: 0");
        assertEquals(t.getQuantity(), 0);

        t = recognizer.recogniseNonCardToken("CC1");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_CMC);
        assertEquals(t.getText(), "CMC: 1");
        assertEquals(t.getQuantity(), 0);

        t = recognizer.recogniseNonCardToken("//Common");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_RARITY);
        assertEquals(t.getText(), "Common");
        assertEquals(t.getQuantity(), 0);

        t = recognizer.recogniseNonCardToken("(mythic rare)");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.CARD_RARITY);
        assertEquals(t.getText(), "mythic rare");
        assertEquals(t.getQuantity(), 0);

        t = recognizer.recogniseNonCardToken("//Blue");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.MANA_COLOUR);
        assertEquals(t.getText(), String.format("%s {U}", ForgeCardMockTestCase.MOCKED_LOCALISED_STRING));
        assertEquals(t.getQuantity(), 0);

        t = recognizer.recogniseNonCardToken("(Colorless)");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.MANA_COLOUR);
        assertEquals(t.getText(), String.format("%s {C}", ForgeCardMockTestCase.MOCKED_LOCALISED_STRING));
        assertEquals(t.getQuantity(), 0);

        t = recognizer.recogniseNonCardToken("//Planes");
        assertNotNull(t);
        assertEquals(t.getType(), TokenType.DECK_SECTION_NAME);
        assertEquals(t.getText(), "Planes");
        assertEquals(t.getQuantity(), 0);
    }

    /*=============================
     * TEST RECOGNISE CARD LINES
     * =============================*/

    // === Card-Set Pattern Request
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

    // === Set-Card Pattern Request
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

    //=== Card-Set-CollectorNumber Pattern Request
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

        validRequest = "Power Sink (TMP)|78";  // Pipe to separate collector number (as in .Dec files)
        matcher = DeckRecognizer.CARD_SET_COLLNO_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink ");  // TRIM
        assertEquals(matcher.group(DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(matcher.group(DeckRecognizer.REGRP_COLLNR), "78");

        validRequest = "Power Sink|TMP|78";  // .Dec file export entry format
        matcher = DeckRecognizer.CARD_SET_COLLNO_PATTERN.matcher(validRequest);
        assertTrue(matcher.matches());
        assertNull(matcher.group(DeckRecognizer.REGRP_CARDNO));
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Power Sink");  // TRIM
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

    // === Set-Card-CollectorNumber Pattern Request
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

        validRequest = "(TMP} Power Sink|78";  // Pipe to separate collector number as in .Dec format
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

    // === Card Only Pattern Request (No Set)
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

    @Test void testRecogniseCardToken(){
        DeckRecognizer recognizer = new DeckRecognizer();

        String lineRequest = "4x Power Sink+ (TMP) 78";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getTokenSection());
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getCard());
        PaperCard tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertTrue(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertEquals(tokenCard.getCollectorNumber(), "78");
        assertFalse(cardToken.cardRequestHasNoCode());

        lineRequest = "4x Power Sink TMP 78";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertFalse(cardToken.cardRequestHasNoCode());
        assertEquals(tokenCard.getCollectorNumber(), "78");

        lineRequest = "4x TMP Power Sink 78";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertEquals(tokenCard.getCollectorNumber(), "78");
        assertFalse(cardToken.cardRequestHasNoCode());

        lineRequest = "4x TMP Power Sink";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertEquals(tokenCard.getCollectorNumber(), "78");
        assertFalse(cardToken.cardRequestHasNoCode());

        lineRequest = "4x Power Sink TMP";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertEquals(tokenCard.getCollectorNumber(), "78");
        assertFalse(cardToken.cardRequestHasNoCode());

        lineRequest = "Power Sink TMP";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertEquals(tokenCard.getCollectorNumber(), "78");
        assertFalse(cardToken.cardRequestHasNoCode());

        lineRequest = "[TMP] Power Sink";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertEquals(tokenCard.getCollectorNumber(), "78");
        assertFalse(cardToken.cardRequestHasNoCode());

        // Relax Set Preference
        assertEquals(StaticData.instance().getCommonCards().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        lineRequest = "4x Power Sink";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "VMA");
        assertTrue(cardToken.cardRequestHasNoCode());

        lineRequest = "4x Power Sink+";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertTrue(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "VMA");
        assertTrue(cardToken.cardRequestHasNoCode());

        lineRequest = "Power Sink+";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertTrue(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "VMA");
        assertTrue(cardToken.cardRequestHasNoCode());
    }

    @Test void testSingleWordCardNameMatchesCorrectly(){
        DeckRecognizer recognizer = new DeckRecognizer();

        String lineRequest = "2x Counterspell ICE";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        PaperCard tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 2);
        assertEquals(tokenCard.getName(), "Counterspell");
        assertEquals(tokenCard.getEdition(), "ICE");
        assertFalse(cardToken.cardRequestHasNoCode());

        // Remove Set code
        assertEquals(StaticData.instance().getCommonCards().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        lineRequest = "2x Counterspell";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 2);
        assertEquals(tokenCard.getName(), "Counterspell");
        assertEquals(tokenCard.getEdition(), "MH2");
        assertTrue(cardToken.cardRequestHasNoCode());

    }

    /*=========================================
     * TOKEN/CARD PARSING: (Alternate) Set Code
     * ======================================== */
    @Test void testRecognisingCardFromSetUsingAlternateCode(){
        DeckRecognizer recognizer = new DeckRecognizer();

        String lineRequest = "4x Power Sink+ TE 78";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertNotNull(cardToken.getTokenSection());
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertTrue(cardToken.isCardToken());
        PaperCard tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertTrue(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "TMP");
        assertEquals(tokenCard.getCollectorNumber(), "78");
        assertFalse(cardToken.cardRequestHasNoCode());
    }

    /*==================================
     * TOKEN/CARD PARSING: Foil Requests
     * ================================= */
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

    /*===================================================
     * TOKEN/CARD PARSING: Collector Number and Art Index
     * ================================================== */
    @Test void testPassingInArtIndexRatherThanCollectorNumber(){
        DeckRecognizer recognizer = new DeckRecognizer();

        String lineRequest = "20x Mountain MIR 3";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        PaperCard tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 20);
        assertEquals(tokenCard.getName(), "Mountain");
        assertEquals(tokenCard.getEdition(), "MIR");
        assertEquals(tokenCard.getArtIndex(), 3);
        assertEquals(tokenCard.getCollectorNumber(), "345");
        assertFalse(cardToken.cardRequestHasNoCode());
    }

    @Test void testCollectorNumberIsNotConfusedAsArtIndexInstead(){
        DeckRecognizer recognizer = new DeckRecognizer();

        String lineRequest = "2x Auspicious Ancestor MIR 3";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        PaperCard tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 2);
        assertEquals(tokenCard.getName(), "Auspicious Ancestor");
        assertEquals(tokenCard.getEdition(), "MIR");
        assertEquals(tokenCard.getArtIndex(), 1);
        assertEquals(tokenCard.getCollectorNumber(), "3");
        assertFalse(cardToken.cardRequestHasNoCode());
    }

    @Test void testCardRequestWithWrongCollectorNumberStillReturnsTheCardFromSetIfAny(){
        DeckRecognizer recognizer = new DeckRecognizer();

        String requestLine = "3 Jayemdae Tome (LEB) 231";  // actually found in TappedOut Deck Export
        // NOTE: Expected Coll Nr should be 255
        Token cardToken = recognizer.recogniseCardToken(requestLine, null);
        assertNotNull(cardToken);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 3);
        PaperCard card = cardToken.getCard();
        assertEquals(card.getName(), "Jayemdae Tome");
        assertEquals(card.getEdition(), "LEB");
        assertEquals(card.getCollectorNumber(), "255");
        assertFalse(cardToken.cardRequestHasNoCode());

        // No Match - Unknown card
        requestLine = "3 Jayemdae Tome (TMP)";  // actually found in TappedOut Deck Export
        // NOTE: Expected Coll Nr should be 255
        cardToken = recognizer.recogniseCardToken(requestLine, null);
        assertNotNull(cardToken);
        assertNull(cardToken.getCard());
        assertNull(cardToken.getTokenSection());
        assertEquals(cardToken.getType(), TokenType.UNKNOWN_CARD);
    }

    /*=================================
     * TOKEN/CARD PARSING: UNKNOWN CARD
     * ================================ */
    @Test void testRequestingCardFromTheWrongSetReturnsUnknownCard(){
        DeckRecognizer recognizer = new DeckRecognizer();

        String lineRequest = "2x Counterspell FEM";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.UNKNOWN_CARD);
        assertNull(cardToken.getCard());
    }

    @Test void testRequestingCardFromNonExistingSetReturnsUnknownCard(){
        DeckRecognizer recognizer = new DeckRecognizer();

        String lineRequest = "2x Counterspell BOU";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.UNKNOWN_CARD);
        assertNull(cardToken.getCard());
        assertNull(cardToken.getTokenSection());
    }

    /*=======================================
     * TEST CONSTRAINTS: Edition Release Date
     * ====================================== */
    @Test void testRequestingCardWithReleaseDateConstraints(){
        DeckRecognizer recognizer = new DeckRecognizer();
        recognizer.setDateConstraint(2002, 1);
        assertEquals(StaticData.instance().getCommonCards().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String lineRequest = "Ancestral Recall";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        PaperCard ancestralCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(ancestralCard.getName(), "Ancestral Recall");
        assertEquals(StaticData.instance().getCommonCards().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        assertEquals(ancestralCard.getEdition(), "2ED");
        assertTrue(cardToken.cardRequestHasNoCode());

        // Counterspell editions
        lineRequest = "Counterspell";
        recognizer.setDateConstraint(1999, 10);
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        PaperCard counterSpellCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(counterSpellCard.getName(), "Counterspell");
        assertEquals(counterSpellCard.getEdition(), "MMQ");
        assertTrue(cardToken.cardRequestHasNoCode());
    }

    @Test void testInvalidCardRequestWhenReleaseDateConstraintsAreUp(){
        DeckRecognizer recognizer = new DeckRecognizer();

        assertEquals(StaticData.instance().getCommonCards().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        // First run without constraint to check that it's a valid request
        String lineRequest = "Counterspell|MH2";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        PaperCard counterSpellCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(counterSpellCard.getName(), "Counterspell");
        assertEquals(counterSpellCard.getEdition(), "MH2");
        assertFalse(cardToken.cardRequestHasNoCode());

        recognizer.setDateConstraint(1999, 10);
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.CARD_FROM_INVALID_SET);
        assertNotNull(cardToken.getCard());
        assertFalse(cardToken.cardRequestHasNoCode());
    }

    /*======================================
     * TEST CONSTRAINTS: Card Art Preference
     * ===================================== */
    @Test void testChangesInArtPreference(){
        DeckRecognizer recognizer = new DeckRecognizer();

        // Baseline - no constraints - uses default card art
        assertEquals(StaticData.instance().getCommonCards().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        String lineRequest = "Counterspell";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 1);
        assertNotNull(cardToken.getTokenSection());
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        PaperCard tc = cardToken.getCard();
        assertEquals(tc.getName(), "Counterspell");
        assertEquals(tc.getEdition(), "MH2");
        assertTrue(cardToken.cardRequestHasNoCode());

        // Setting Original Core
        recognizer.setArtPreference(CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 1);
        assertNotNull(cardToken.getTokenSection());
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        tc = cardToken.getCard();
        assertEquals(tc.getName(), "Counterspell");
        assertEquals(tc.getEdition(), "LEA");
        assertTrue(cardToken.cardRequestHasNoCode());
    }

    @Test void testCardRequestVariesUponChangesInArtPreference(){
        assertEquals(StaticData.instance().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        DeckRecognizer recognizer = new DeckRecognizer();

        String lineRequest = "4x Power Sink+";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        PaperCard tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertTrue(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "VMA");
        assertTrue(cardToken.cardRequestHasNoCode());

        recognizer.setArtPreference(CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY);
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertTrue(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "LEA");
        assertTrue(cardToken.cardRequestHasNoCode());

        // Check that result is persistent - and consistent with change
        lineRequest = "Power Sink";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        tokenCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(tokenCard.getName(), "Power Sink");
        assertFalse(tokenCard.isFoil());
        assertEquals(tokenCard.getEdition(), "LEA");
        assertTrue(cardToken.cardRequestHasNoCode());
    }

    /*==============================
     * TEST CONSTRAINTS: Game Format
     * ============================= */

    @Test void testRequestingCardWithRestrictionsOnSetsFromGameFormat(){
        DeckRecognizer recognizer = new DeckRecognizer();
        // Setting Fantasy Constructed Game Format: Urza's Block Format
        List<String> allowedSets = Arrays.asList("USG", "ULG", "UDS", "PUDS", "PULG", "PUSG");
        recognizer.setGameFormatConstraint(allowedSets, null, null);

        String lineRequest = "2x Counterspell ICE";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.CARD_FROM_NOT_ALLOWED_SET);
        assertNotNull(cardToken.getCard());
        assertNull(cardToken.getTokenSection());
        assertFalse(cardToken.cardRequestHasNoCode());

        lineRequest = "2x Counterspell";  // It does not exist any Counterspell in Urza's block
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.CARD_FROM_NOT_ALLOWED_SET);
        assertNotNull(cardToken.getCard());
        assertNull(cardToken.getTokenSection());
        assertTrue(cardToken.cardRequestHasNoCode());
    }

    @Test void testRequestingCardWithRestrictionsOnDeckFormat(){
        DeckRecognizer recognizer = new DeckRecognizer();

        String lineRequest = "Ancestral Recall";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        PaperCard ancestralCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(ancestralCard.getName(), "Ancestral Recall");
        assertEquals(StaticData.instance().getCommonCards().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        assertEquals(ancestralCard.getEdition(), "VMA");
        assertTrue(cardToken.cardRequestHasNoCode());

        recognizer.setDeckFormatConstraint(DeckFormat.TinyLeaders);
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LIMITED_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getLimitedCardType());
        assertEquals(cardToken.getLimitedCardType(), DeckRecognizer.LimitedCardType.BANNED);
        assertTrue(cardToken.cardRequestHasNoCode());
    }

    @Test void testCardRequestUnderGameConstraints(){
        // == Simulate Pioneer Format Banned List
        DeckRecognizer recognizer = new DeckRecognizer();
        List<String> bannedList = Arrays.asList(
                StringUtils.split("Balustrade Spy;Bloodstained Mire;Felidar Guardian;Field of the Dead;Flooded Strand;Inverter of Truth;Kethis, the Hidden Hand;Leyline of Abundance;Nexus of Fate;Oko, Thief of Crowns;Once Upon a Time;Polluted Delta;Smuggler's Copter;Teferi, Time Raveler;Undercity Informer;Underworld Breach;Uro, Titan of Nature's Wrath;Veil of Summer;Walking Ballista;Wilderness Reclamation;Windswept Heath;Wooded Foothills",
                        ';'));
        List<String> allowedPioneerSets = Arrays.asList(
                StringUtils.split("RTR,GTC,DGM,M14,THS,BNG,JOU,M15,KTK,FRF,DTK,ORI,BFZ,OGW,SOI,EMN,KLD,AER,AKH,HOU,XLN,RIX,DOM,M19,G18,GRN,RNA,WAR,M20,ELD,THB,IKO,M21,ZNR,KHM,STX,AFR,MID,VOW",
                        ","));
        List<String> restrictedList = null;

        recognizer.setGameFormatConstraint(allowedPioneerSets, bannedList, restrictedList);

        String cardRequest = "4 ONS Bloodstained Mire";
        Token cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.CARD_FROM_NOT_ALLOWED_SET);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 4);
        assertNull(cardToken.getTokenSection());
        assertEquals(cardToken.getCard().getName(), "Bloodstained Mire");
        assertEquals(cardToken.getCard().getEdition(), "ONS");
        assertEquals(cardToken.getText(), "Bloodstained Mire [ONS] #313");
        assertFalse(cardToken.cardRequestHasNoCode());

        String noEditionCardRequest = "4 Bloodstained Mire";
        cardToken = recognizer.recogniseCardToken(noEditionCardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LIMITED_CARD);
        assertEquals(cardToken.getLimitedCardType(), DeckRecognizer.LimitedCardType.BANNED);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 4);
        assertNotNull(cardToken.getTokenSection());
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertEquals(cardToken.getCard().getName(), "Bloodstained Mire");
        assertEquals(cardToken.getCard().getEdition(), "KTK");
        assertEquals(cardToken.getText(), "Bloodstained Mire [KTK] #230");
        assertTrue(cardToken.cardRequestHasNoCode());
    }

    @Test void testGameFormatRestrictionsAlsoWithRestrictedCardList(){
        // SIMULATE A GAME OF VINTAGE
        DeckRecognizer recognizer = new DeckRecognizer();
        List<String> allowedSetCodes = Arrays.asList(
                StringUtils.split("7ED, 9ED, ORI, M14, M15, 6ED, 8ED, M11, 3ED, M10, M12, 10E, M13, G18, M21, M20, M19, 5ED, 2ED, 4ED, LEB, LEA, 5DN, SOM, KTK, THS, DIS, JOU, MOR, TMP, SOI, FEM, USG, ALL, ROE, EXO, TSP, LRW, TOR, ALA, RIX, DGM, DKA, MBS, AER, RNA, GTC, CSP, HML, NPH, OGW, ZNR, EMN, UDS, SHM, BNG, SOK, EVE, INV, THB, DOM, NMS, VIS, WAR, GRN, PCY, SCG, MRD, XLN, ONS, IKO, MMQ, CHK, ULG, AKH, MIR, ISD, AVR, KLD, APC, RTR, WWK, PLC, HOU, LEG, AFR, ARN, ICE, STX, LGN, ARB, KHM, CFX, TSB, ZEN, ELD, JUD, GPT, BFZ, BOK, DTK, FRF, FUT, WTH, ODY, RAV, ATQ, DRK, PLS, STH, DST, TD2, HA1, ME4, HA3, HA2, HA5, HA4, MED, ANB, ME3, KLR, PZ2, ANA, PRM, PZ1, AJMP, ME2, TD1, TD0, TPR, VMA, AKR, MBP, PZEN, PGTW, PL21, PFUT, PWAR, PAL01, PJUD, PAL00, PTKDF, PWOR, PWP12, PSTH, POGW, PFRF, PG07, PSUS, PUST, J18, PWP10, PAL02, PAL03, PWP11, J19, PGRN, PM10, PDP14, PRTR, PMPS06, PBNG, PJ21, G09, PNPH, PM15, PAL06, G08, PDST, J20, PMBS, PMPS07, PEXO, PDOM, PONS, PRW2, PMPS11, PMPS, PM19, PWWK, PCEL, PAL04, PAL05, PMPS10, PDTK, PALP, F10, F04, PMOR, PAL99, PEMN, PCNS, PPLC, PRAV, PPP1, PI14, PXLN, PF20, PTSP, F05, F11, PSCG, PBOOK, F07, F13, PODY, PM12, P08, PSS1, P2HG, P09, PTOR, PDP13, F12, F06, PALA, PXTC, F02, F16, PHOU, PSOM, PI13, PCON, PDGM, PIDW, PMRD, PRNA, P9ED, PHEL, F17, F03, PURL, F15, F01, PWOS, PPC1, PBOK, PTMP, PS19, PS18, PF19, PGPT, PCHK, FNM, F14, PISD, PAKH, PDP15, PRIX, PS15, PPCY, OLGC, OVNT, PLGN, PS14, P03, PDTP, PM14, FS, PPLS, MPR, PKTK, PS16, PRWK, PS17, PBFZ, PSS2, PINV, G03, P8ED, PARL, P04, P10, PSDC, JGP, G99, WW, P11, P05, PDIS, PROE, PDP10, F08, P10E, PELP, PMH1, P07, P5DN, PGRU, SHC, PM11, P06, PUSG, PCMP, PULG, F09, PUDS, PARB, DRC94, PMPS09, PORI, J12, G06, PMMQ, G07, J13, PMPS08, PM20, PSOI, PJSE, G05, G11, PNAT, PSOK, PEVE, PRED, G10, G04, PSHM, PPRO, PAPC, PJJT, ARENA, PKLD, G00, J14, PLGM, P15A, PCSP, PWPN, PJAS, PWP21, PWP09, PDKA, PNEM, PPTK, J15, G01, PG08, PLRW, PMEI, PM13, PHJ, PGTC, J17, PRES, PWCQ, PJOU, PDP12, PAER, PAVR, PTHS, G02, J16, PSUM, PGPX, UGF, PSS3, MM2, MM3, MB1, FMB1, A25, 2XM, MMA, PLIST, CHR, EMA, IMA, TSR, UMA, PUMA, E02, DPA, ATH, MD1, GK1, GK2, CST, BRB, BTD, DKM, FVE, V17, V13, STA, MPS_RNA, V16, SLD, V12, CC1, MPS_GRN, DRB, FVR, SS3, SS1, MPS_AKH, FVL, V15, MPS_KLD, ZNE, PDS, SS2, PD3, SLU, V14, PD2, EXP, MPS_WAR, DDQ, DDE, GS1, DDS, DDU, DD1, DDL, DDF, DDP, DD2, DDR, DDH, DDT, DDK, DDG, DDC, DDM, DDJ, DDO, GVL, JVC, DDI, DVD, DDN, EVG, DDD, C18, C19, C21, C20, C13, CMA, C14, C15, KHC, ZNC, AFC, C17, C16, COM, CM1,CM2,PO2,S99,W16,W17,S00,PTK,CP3,POR,CP1,CP2,CMR,MH2,H1R,CNS,BBD,MH1,CN2,JMP,PCA,GNT,ARC,GN2,PC2,E01,HOP,PLG20,PLG21,CC2,MID,MIC,VOW,VOC",
                        ","));
        allowedSetCodes = allowedSetCodes.stream().map(String::trim).collect(Collectors.toList());
        List<String> bannedCards = Arrays.asList(
                StringUtils.split("Adriana's Valor; Advantageous Proclamation; Assemble the Rank and Vile; Backup Plan; Brago's Favor; Double Stroke; Echoing Boon; Emissary's Ploy; Hired Heist; Hold the Perimeter; Hymn of the Wilds; Immediate Action; Incendiary Dissent; Iterative Analysis; Muzzio's Preparations; Natural Unity; Power Play; Secret Summoning; Secrets of Paradise; Sentinel Dispatch; Sovereign's Realm; Summoner's Bond; Unexpected Potential; Weight Advantage; Worldknit; Amulet of Quoz; Bronze Tablet; Contract from Below; Darkpact; Demonic Attorney; Jeweled Bird; Rebirth; Tempest Efreet; Timmerian Fiends; Chaos Orb; Falling Star; Shahrazad; Cleanse; Crusade; Imprison; Invoke Prejudice; Jihad; Pradesh Gypsies; Stone-Throwing Devils",
                        ":"));
        bannedCards = bannedCards.stream().map(String::trim).collect(Collectors.toList());
        List<String> restrictedCards = Arrays.asList(
                StringUtils.split("Ancestral Recall; Balance; Black Lotus; Brainstorm; Chalice of the Void; Channel; Demonic Consultation; Demonic Tutor; Dig Through Time; Flash; Gitaxian Probe; Golgari Grave-Troll; Gush; Imperial Seal; Karn, the Great Creator; Library of Alexandria; Lion's Eye Diamond; Lodestone Golem; Lotus Petal; Mana Crypt; Mana Vault; Memory Jar; Mental Misstep; Merchant Scroll; Mind's Desire; Monastery Mentor; Mox Emerald; Mox Jet; Mox Pearl; Mox Ruby; Mox Sapphire; Mystic Forge; Mystical Tutor; Narset, Parter of Veils; Necropotence; Ponder; Sol Ring; Strip Mine; Thorn of Amethyst; Time Vault; Time Walk; Timetwister; Tinker; Tolarian Academy; Treasure Cruise; Trinisphere; Vampiric Tutor; Wheel of Fortune; Windfall; Yawgmoth's Will",
                        ";"));
        restrictedCards = restrictedCards.stream().map(String::trim).collect(Collectors.toList());

        recognizer.setGameFormatConstraint(allowedSetCodes, bannedCards, restrictedCards);

        assertEquals(StaticData.instance().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String cardRequest = "Ancestral Recall";
        Token cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Ancestral Recall");
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(cardToken.getCard().getEdition(), "VMA");
        assertTrue(cardToken.cardRequestHasNoCode());

        cardRequest = "4x Ancestral Recall";
        cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LIMITED_CARD);
        assertEquals(cardToken.getLimitedCardType(), DeckRecognizer.LimitedCardType.RESTRICTED);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Ancestral Recall");
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(cardToken.getCard().getEdition(), "VMA");
        assertTrue(cardToken.cardRequestHasNoCode());
    }

    @Test void testSettingPartialConstraintsOnGameFormatsAreStillApplied(){
        // Setting only Partial Game Constraints
        DeckRecognizer recognizer = new DeckRecognizer();
        List<String> allowedSetCodes = Arrays.asList("MIR", "VIS", "WTH");
        List<String> bannedCards = Collections.singletonList("Squandered Resources");
        List<String> restrictedCards = Collections.singletonList("Viashino Sandstalker");

        assertEquals(StaticData.instance().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        // == RESTRICTED CARDS ONLY
        recognizer.setGameFormatConstraint(null, null, restrictedCards);

        String cardRequest = "Viashino Sandstalker";
        Token cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Viashino Sandstalker");
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(cardToken.getCard().getEdition(), "MB1");
        assertTrue(cardToken.cardRequestHasNoCode());

        cardRequest = "4x Viashino Sandstalker";
        cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LIMITED_CARD);
        assertEquals(cardToken.getLimitedCardType(), DeckRecognizer.LimitedCardType.RESTRICTED);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Viashino Sandstalker");
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(cardToken.getCard().getEdition(), "MB1");
        assertTrue(cardToken.cardRequestHasNoCode());

        // Requesting now what will be a Banned card later in this test
        cardRequest = "Squandered Resources";
        cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Squandered Resources");
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(cardToken.getCard().getEdition(), "VIS");
        assertTrue(cardToken.cardRequestHasNoCode());

        // == ALLOWED SETS ONLY
        recognizer.setGameFormatConstraint(allowedSetCodes, null, null);

        cardRequest = "4x Viashino Sandstalker";
        cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Viashino Sandstalker");
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(cardToken.getCard().getEdition(), "VIS");
        assertTrue(cardToken.cardRequestHasNoCode());

        // == BANNED CARDS ONLY
        recognizer.setGameFormatConstraint(null, bannedCards, null);

        cardRequest = "4x Viashino Sandstalker";
        cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Viashino Sandstalker");
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(cardToken.getCard().getEdition(), "MB1");
        assertTrue(cardToken.cardRequestHasNoCode());

        cardRequest = "Squandered Resources";
        cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LIMITED_CARD);
        assertNotNull(cardToken.getLimitedCardType());
        assertEquals(cardToken.getLimitedCardType(), DeckRecognizer.LimitedCardType.BANNED);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Squandered Resources");
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(cardToken.getCard().getEdition(), "VIS");
        assertTrue(cardToken.cardRequestHasNoCode());

        // ALLOWED SET CODES AND RESTRICTED
        recognizer.setGameFormatConstraint(allowedSetCodes, null, restrictedCards);

        cardRequest = "4x Viashino Sandstalker";
        cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LIMITED_CARD);
        assertEquals(cardToken.getLimitedCardType(), DeckRecognizer.LimitedCardType.RESTRICTED);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Viashino Sandstalker");
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(cardToken.getCard().getEdition(), "VIS");
        assertTrue(cardToken.cardRequestHasNoCode());
    }

    /*======================================
     * TEST CONSTRAINTS: Combined Constraints
     * ===================================== */

    @Test void testCardMatchWithDateANDGameFormatConstraints(){
        DeckRecognizer recognizer = new DeckRecognizer();

        // Baseline - no constraints
        assertEquals(StaticData.instance().getCommonCards().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        String lineRequest = "2x Lightning Dragon";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 2);
        PaperCard tc = cardToken.getCard();
        assertEquals(tc.getName(), "Lightning Dragon");
        assertEquals(tc.getEdition(), "VMA");
        assertTrue(cardToken.cardRequestHasNoCode());

        recognizer.setDateConstraint(2000, 0);  // Jan 2000
        // Setting Fantasy Constructed Game Format: Urza's Block Format (no promo)
        List<String> allowedSets = Arrays.asList("USG", "ULG", "UDS");
        recognizer.setGameFormatConstraint(allowedSets, null, null);

        lineRequest = "2x Lightning Dragon|USG";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 2);
        tc = cardToken.getCard();
        assertEquals(tc.getName(), "Lightning Dragon");
        assertEquals(tc.getEdition(), "USG");
        assertFalse(cardToken.cardRequestHasNoCode());

        // Relaxing Constraint on Set
        lineRequest = "2x Lightning Dragon";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getQuantity(), 2);
        assertNotNull(cardToken.getCard());
        tc = cardToken.getCard();
        assertEquals(tc.getName(), "Lightning Dragon");
        assertEquals(tc.getEdition(), "USG");  // the latest available within set requested
        assertTrue(cardToken.cardRequestHasNoCode());

        // Now setting a tighter date constraint
        recognizer.setDateConstraint(1998, 0);  // Jan 1998
        lineRequest = "2x Lightning Dragon|USG";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.CARD_FROM_INVALID_SET);
        assertEquals(cardToken.getQuantity(), 2);
        assertNotNull(cardToken.getCard());
        assertFalse(cardToken.cardRequestHasNoCode());

        lineRequest = "2x Lightning Dragon";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.CARD_FROM_INVALID_SET);
        assertEquals(cardToken.getQuantity(), 2);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Lightning Dragon [USG] #202");
        assertTrue(cardToken.cardRequestHasNoCode());

        // Now relaxing date constraint but removing USG from allowed sets
        // VMA release date: 2014-06-16
        recognizer.setDateConstraint(2015, 0);  // This will match VMA
        recognizer.setGameFormatConstraint(Arrays.asList("ULG", "UDS"), null, null);

        lineRequest = "2x Lightning Dragon|USG";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.CARD_FROM_NOT_ALLOWED_SET);
        assertEquals(cardToken.getQuantity(), 2);
        assertNotNull(cardToken.getCard());
        assertFalse(cardToken.cardRequestHasNoCode());

        lineRequest = "2x Lightning Dragon";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.CARD_FROM_NOT_ALLOWED_SET);
        assertEquals(cardToken.getQuantity(), 2);
        assertNotNull(cardToken.getCard());
        assertTrue(cardToken.cardRequestHasNoCode());

        // Now relaxing date constraint but removing USG from allowed sets
        // VMA release date: 2014-06-16
        recognizer.setDateConstraint(2015, 0);  // This will match VMA
        recognizer.setGameFormatConstraint(Arrays.asList("VMA", "ULG", "UDS"), null, null);
        lineRequest = "2x Lightning Dragon";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getQuantity(), 2);
        assertNotNull(cardToken.getCard());
        tc = cardToken.getCard();
        assertEquals(tc.getName(), "Lightning Dragon");
        assertEquals(tc.getEdition(), "VMA");
        assertTrue(cardToken.cardRequestHasNoCode());
    }

    @Test void testCardMatchWithDateANDdeckFormatConstraints(){
        DeckRecognizer recognizer = new DeckRecognizer();

        // Baseline - no constraints
        assertEquals(StaticData.instance().getCommonCards().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String lineRequest = "Flash";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 1);
        PaperCard tc = cardToken.getCard();
        assertEquals(tc.getName(), "Flash");
        assertEquals(tc.getEdition(), "A25");
        assertTrue(cardToken.cardRequestHasNoCode());

        recognizer.setDateConstraint(2012, 0);  // Jan 2012
        recognizer.setDeckFormatConstraint(DeckFormat.TinyLeaders);

        lineRequest = "Flash";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LIMITED_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Flash [6ED] #67");
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getLimitedCardType());
        assertEquals(cardToken.getLimitedCardType(), DeckRecognizer.LimitedCardType.BANNED);
        assertTrue(cardToken.cardRequestHasNoCode());

        lineRequest = "2x Cancel";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getQuantity(), 2);
        assertNotNull(cardToken.getCard());
        tc = cardToken.getCard();
        assertEquals(tc.getName(), "Cancel");
        assertEquals(tc.getEdition(), "M12");  // the latest within date constraint
        assertTrue(cardToken.cardRequestHasNoCode());

        lineRequest = "2x Cancel|M21";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.CARD_FROM_INVALID_SET);
        assertEquals(cardToken.getQuantity(), 2);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Cancel [M21] #46");
        assertFalse(cardToken.cardRequestHasNoCode());
    }

    @Test void testCardMatchWithGameANDdeckFormatConstraints(){
        DeckRecognizer recognizer = new DeckRecognizer();

        // Baseline - no constraints
        assertEquals(StaticData.instance().getCommonCards().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String lineRequest = "Flash";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 1);
        PaperCard tc = cardToken.getCard();
        assertEquals(tc.getName(), "Flash");
        assertEquals(tc.getEdition(), "A25");
        assertTrue(cardToken.cardRequestHasNoCode());

        recognizer.setGameFormatConstraint(Arrays.asList("MIR", "VIS", "WTH"), null, null);
        recognizer.setDeckFormatConstraint(DeckFormat.TinyLeaders);

        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LIMITED_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Flash [MIR] #66");
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getLimitedCardType());
        assertEquals(cardToken.getLimitedCardType(), DeckRecognizer.LimitedCardType.BANNED);
        assertTrue(cardToken.cardRequestHasNoCode());

        lineRequest = "2x Femeref Knight";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getQuantity(), 2);
        assertNotNull(cardToken.getCard());
        tc = cardToken.getCard();
        assertEquals(tc.getName(), "Femeref Knight");
        assertEquals(tc.getEdition(), "MIR");
        assertTrue(cardToken.cardRequestHasNoCode());

        lineRequest = "2x Incinerate";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getQuantity(), 2);
        assertNotNull(cardToken.getCard());
        tc = cardToken.getCard();
        assertEquals(tc.getName(), "Incinerate");
        assertEquals(tc.getEdition(), "MIR");
        assertTrue(cardToken.cardRequestHasNoCode());

        lineRequest = "Noble Elephant";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LIMITED_CARD);  // violating Deck format
        assertEquals(cardToken.getQuantity(), 1);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Noble Elephant [MIR] #30");
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getLimitedCardType());
        assertEquals(cardToken.getLimitedCardType(), DeckRecognizer.LimitedCardType.BANNED);
        assertTrue(cardToken.cardRequestHasNoCode());

        lineRequest = "Incinerate|ICE";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.CARD_FROM_NOT_ALLOWED_SET);  // violating Game format
        assertEquals(cardToken.getQuantity(), 1);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Incinerate [ICE] #194");
        assertFalse(cardToken.cardRequestHasNoCode());
    }

    @Test void testCardMatchWitDateANDgameANDdeckFormatConstraints(){
        DeckRecognizer recognizer = new DeckRecognizer();

        // Baseline - no constraints
        assertEquals(StaticData.instance().getCommonCards().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String lineRequest = "Flash";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 1);
        PaperCard tc = cardToken.getCard();
        assertEquals(tc.getName(), "Flash");
        assertEquals(tc.getEdition(), "A25");
        assertTrue(cardToken.cardRequestHasNoCode());

        recognizer.setGameFormatConstraint(Arrays.asList("MIR", "VIS", "WTH"), null, null);
        recognizer.setDeckFormatConstraint(DeckFormat.TinyLeaders);
        recognizer.setDateConstraint(1999, 2);  // March '99

        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LIMITED_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Flash [MIR] #66");
        assertNotNull(cardToken.getTokenSection());
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getLimitedCardType());
        assertEquals(cardToken.getLimitedCardType(), DeckRecognizer.LimitedCardType.BANNED);
        assertTrue(cardToken.cardRequestHasNoCode());

        lineRequest = "Ardent Militia";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LIMITED_CARD);  // illegal in deck format
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Ardent Militia [WTH] #5");  // within set constraints
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getLimitedCardType());
        assertEquals(cardToken.getLimitedCardType(), DeckRecognizer.LimitedCardType.BANNED);
        assertTrue(cardToken.cardRequestHasNoCode());

        lineRequest = "Buried Alive|UMA";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.CARD_FROM_NOT_ALLOWED_SET);  // illegal in game format
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Buried Alive [UMA] #88");  // within set constraints
        assertFalse(cardToken.cardRequestHasNoCode());

        lineRequest = "Buried Alive";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);  // illegal in deck format
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Buried Alive");
        assertEquals(cardToken.getCard().getEdition(), "WTH");  // within set constraints
        assertTrue(cardToken.cardRequestHasNoCode());

        recognizer.setDateConstraint(1997, 2);  // March '97 - before WTH
        lineRequest = "Buried Alive";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.CARD_FROM_INVALID_SET);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getText(), "Buried Alive [WTH] #63");
        assertTrue(cardToken.cardRequestHasNoCode());
    }

    /*==================================
     * TEST RECOGNISE CARD EXTRA FORMATS
     * =================================*/

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
        assertEquals(matcher.group(DeckRecognizer.REGRP_CARD), "Forest"); // TRIM
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
        DeckRecognizer recognizer = new DeckRecognizer();
        assertEquals(StaticData.instance().getCommonCards().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String lineRequest = "4 Aspect of Hydra [BNG] (F)";
        Token cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        PaperCard aspectOfHydraCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(aspectOfHydraCard.getName(), "Aspect of Hydra");
        assertEquals(aspectOfHydraCard.getEdition(), "BNG");
        assertTrue(aspectOfHydraCard.isFoil());
        assertFalse(cardToken.cardRequestHasNoCode());

        lineRequest = "18 Forest <254> [THB] (F)";
        cardToken = recognizer.recogniseCardToken(lineRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        PaperCard forestCard = cardToken.getCard();
        assertEquals(cardToken.getQuantity(), 18);
        assertEquals(forestCard.getName(), "Forest");
        assertEquals(forestCard.getEdition(), "THB");
        assertTrue(forestCard.isFoil());
        assertFalse(cardToken.cardRequestHasNoCode());
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
        DeckRecognizer recognizer = new DeckRecognizer();
        assertEquals(StaticData.instance().getCommonCards().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String line = "* 1 [Ancestral Recall](http://tappedout.nethttp://tappedout.net/mtg-card/ancestral-recall/)";

        Token token = recognizer.recognizeLine(line, null);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.LEGAL_CARD);
        assertEquals(token.getQuantity(), 1);
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
        DeckRecognizer recognizer = new DeckRecognizer();

        String xmageFormatRequest = "1 [LRW:51] Amoeboid Changeling";
        Token xmageCardToken = recognizer.recogniseCardToken(xmageFormatRequest, null);
        assertNotNull(xmageCardToken);
        assertEquals(xmageCardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(xmageCardToken.getQuantity(), 1);
        assertNotNull(xmageCardToken.getCard());
        PaperCard acCard = xmageCardToken.getCard();
        assertEquals(acCard.getName(), "Amoeboid Changeling");
        assertEquals(acCard.getEdition(), "LRW");
        assertEquals(acCard.getCollectorNumber(), "51");
        assertFalse(xmageCardToken.cardRequestHasNoCode());
    }

    // === Deckstats Commander
    @Test void testRecognizeCommanderCardInDeckstatsExportFormat(){
        DeckRecognizer recognizer = new DeckRecognizer();

        String deckstatsCommanderRequest = "1 Sliver Overlord #!Commander";
        Token deckStatsToken = recognizer.recognizeLine(deckstatsCommanderRequest, null);
        assertNotNull(deckStatsToken);
        assertEquals(deckStatsToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(deckStatsToken.getTokenSection());
        assertEquals(deckStatsToken.getTokenSection(), DeckSection.Commander);
        assertEquals(deckStatsToken.getQuantity(), 1);
        assertNotNull(deckStatsToken.getCard());
        PaperCard soCard = deckStatsToken.getCard();
        assertEquals(soCard.getName(), "Sliver Overlord");
        assertEquals(soCard.getEdition(), "SLD");
        assertEquals(soCard.getCollectorNumber(), "10");
        assertTrue(deckStatsToken.cardRequestHasNoCode());

        // Check that deck section is made effective even if we're currently in Main
        deckStatsToken = recognizer.recognizeLine(deckstatsCommanderRequest, DeckSection.Main);
        assertNotNull(deckStatsToken);
        assertEquals(deckStatsToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(deckStatsToken.getTokenSection());
        assertEquals(deckStatsToken.getTokenSection(), DeckSection.Commander);
        assertEquals(deckStatsToken.getQuantity(), 1);
        assertNotNull(deckStatsToken.getCard());
        soCard = deckStatsToken.getCard();
        assertEquals(soCard.getName(), "Sliver Overlord");
        assertTrue(deckStatsToken.cardRequestHasNoCode());
    }

    // === Double-Sided Cards
    @Test void testRecognizeDoubleSidedCards(){
        String leftSideRequest = "Afflicted Deserter";
        String rightSideRequest = "Werewolf Ransacker";
        String doubleSideRequest = "Afflicted Deserter // Werewolf Ransacker";

        DeckRecognizer recognizer = new DeckRecognizer();

        // Check Left side first
        Token cardToken = recognizer.recogniseCardToken(leftSideRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getCard());
        PaperCard leftSideCard = cardToken.getCard();
        assertEquals(leftSideCard.getName(), leftSideRequest);
        assertEquals(cardToken.getQuantity(), 1);
        assertTrue(cardToken.cardRequestHasNoCode());

        // Check Right side first
        cardToken = recognizer.recogniseCardToken(rightSideRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getCard());
        PaperCard rightSideCard = cardToken.getCard();
        assertEquals(rightSideCard.getName(), leftSideRequest);  // NOTE: this is not a blunder! Back side will result in front side name
        assertEquals(cardToken.getQuantity(), 1);
        assertTrue(cardToken.cardRequestHasNoCode());

        // Check double side
        cardToken = recognizer.recogniseCardToken(doubleSideRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getCard());
        PaperCard doubleSideCard = cardToken.getCard();
        assertEquals(doubleSideCard.getName(), leftSideRequest);
        assertEquals(cardToken.getQuantity(), 1);
        assertTrue(cardToken.cardRequestHasNoCode());
    }

    /*=================================
     * TEST CARD TOKEN SECTION MATCHING
     * ================================ */
    @Test void testCardTokenIsAssignedToCorrectDeckSection(){
        DeckRecognizer recognizer = new DeckRecognizer();

        String cardRequest = "2x Counterspell |TMP";
        Token cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Counterspell");
        assertEquals(cardToken.getCard().getEdition(), "TMP");
        assertEquals(cardToken.getQuantity(), 2);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);

        cardToken = recognizer.recogniseCardToken(cardRequest, DeckSection.Main);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 2);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);

        cardToken = recognizer.recogniseCardToken(cardRequest, DeckSection.Sideboard);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 2);
        assertEquals(cardToken.getTokenSection(), DeckSection.Sideboard);

        // Setting Deck Section in Card Request now
        cardRequest = "SB: 2x Counterspell|TMP";
        cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Counterspell");
        assertEquals(cardToken.getCard().getEdition(), "TMP");
        assertEquals(cardToken.getQuantity(), 2);
        assertEquals(cardToken.getTokenSection(), DeckSection.Sideboard);

        cardToken = recognizer.recogniseCardToken(cardRequest, DeckSection.Main);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 2);
        assertEquals(cardToken.getTokenSection(), DeckSection.Sideboard);
    }

    @Test void testCardSectionIsAdaptedToCardRegardlessOfCurrentSection(){
        DeckRecognizer recognizer = new DeckRecognizer();

        String cardRequest = "2x All in good time";  // Scheme Card
        Token cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "All in Good Time");
        assertEquals(cardToken.getQuantity(), 2);
        assertEquals(cardToken.getTokenSection(), DeckSection.Schemes);

        cardToken = recognizer.recogniseCardToken(cardRequest, DeckSection.Main);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "All in Good Time");
        assertEquals(cardToken.getQuantity(), 2);
        assertEquals(cardToken.getTokenSection(), DeckSection.Schemes);
    }

    @Test void testCardSectionIsAdpatedToCardRegardlessOfSectionInCardRequest(){
        DeckRecognizer recognizer = new DeckRecognizer();

        String cardRequest = "CM: 4x Incinerate";  // Incinerate in Commander Section
        Token cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Incinerate");
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);

        // Current Deck Section is Sideboard, so Side should be used as replacing Deck Section
        cardToken = recognizer.recogniseCardToken(cardRequest, DeckSection.Sideboard);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Incinerate");
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(cardToken.getTokenSection(), DeckSection.Sideboard);
    }

    @Test void testDeckSectionTokenValidationAlsoAppliesToNonLegalCards(){
        DeckRecognizer recognizer = new DeckRecognizer();
        recognizer.setGameFormatConstraint(null, Collections.singletonList("Incinerate"), null);

        String cardRequest = "CM: 4x Incinerate";  // Incinerate in Commander Section
        Token cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LIMITED_CARD);
        assertNotNull(cardToken.getLimitedCardType());
        assertEquals(cardToken.getLimitedCardType(), DeckRecognizer.LimitedCardType.BANNED);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Incinerate");
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);

        // Current Deck Section is Sideboard, so Side should be used as replacing Deck Section
        cardToken = recognizer.recogniseCardToken(cardRequest, DeckSection.Sideboard);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LIMITED_CARD);
        assertNotNull(cardToken.getLimitedCardType());
        assertEquals(cardToken.getLimitedCardType(), DeckRecognizer.LimitedCardType.BANNED);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Incinerate");
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(cardToken.getTokenSection(), DeckSection.Sideboard);
    }

    @Test void testCornerCaseWhenThereIsNoCurrentSectionAndMatchedSectionIsNotAllowedButCouldMatchMain(){
        DeckRecognizer recognizer = new DeckRecognizer();

        String cardRequest = "All in Good Time"; // Scheme Section
        Token cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getTokenSection());
        assertEquals(cardToken.getTokenSection(), DeckSection.Schemes);

        // Matches Scheme regardless current is Main
        cardRequest = "All in Good Time"; // Scheme Section
        cardToken = recognizer.recogniseCardToken(cardRequest, DeckSection.Main);
        assertNotNull(cardToken);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getTokenSection());
        assertEquals(cardToken.getTokenSection(), DeckSection.Schemes);

        // This Commander Card can go to Main, current is Main, so it will go there!
        cardRequest = "1 Anowon, the Ruin Thief"; // Commander Section
        cardToken = recognizer.recogniseCardToken(cardRequest, DeckSection.Main);
        assertNotNull(cardToken);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getTokenSection());
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);

        // Now with no current section, commander will match
        cardRequest = "1 Anowon, the Ruin Thief"; // Commander Section
        cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getTokenSection());
        assertEquals(cardToken.getTokenSection(), DeckSection.Commander);

        // Now add constraint
        recognizer.setAllowedDeckSections(Arrays.asList(DeckSection.Main, DeckSection.Sideboard));

        // Now with no current section, commander will match
        // but it's not allowed, so Main will be tried and returned
        cardRequest = "1 Anowon, the Ruin Thief"; // Commander Section
        cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getTokenSection());
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);

        // Scheme is not allowed, but the card won't match Main so Scheme will be
        // returned anyway, so that it can become "UNsupported card" later.
        cardRequest = "All in Good Time"; // Scheme Section
        cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getTokenSection());
        assertEquals(cardToken.getTokenSection(), DeckSection.Schemes);
    }

    /*=================================
     * TEST UNKNOWN CARDS
     * ================================ */

    @Test void testUknonwCardIsReturnedForAnExistingCardFromTheWrongSet(){
        String cardRequest = "Counterspell FEM";
        DeckRecognizer recognizer = new DeckRecognizer();
        Token unknonwCardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(unknonwCardToken);
        assertEquals(unknonwCardToken.getType(), TokenType.UNKNOWN_CARD);
        assertNull(unknonwCardToken.getCard());
        assertNull(unknonwCardToken.getTokenSection());
    }

    @Test void testUknownCardIsReturnedForLineRequestsThatLooksLikeACardButAreNotSupported(){
        String cardRequest = "2x Counterspelling TMP";
        DeckRecognizer recognizer = new DeckRecognizer();
        Token unknonwCardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(unknonwCardToken);
        assertEquals(unknonwCardToken.getType(), TokenType.UNKNOWN_CARD);
        assertNull(unknonwCardToken.getCard());
        assertNull(unknonwCardToken.getTokenSection());

        cardRequest = "2x Counterspelling";
        unknonwCardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(unknonwCardToken);
        assertEquals(unknonwCardToken.getType(), TokenType.UNKNOWN_CARD);
        assertNull(unknonwCardToken.getCard());
        assertNull(unknonwCardToken.getTokenSection());

        cardRequest = "2x Counterspell FEM ";
        unknonwCardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(unknonwCardToken);
        assertEquals(unknonwCardToken.getType(), TokenType.UNKNOWN_CARD);
        assertNull(unknonwCardToken.getCard());
        assertNull(unknonwCardToken.getTokenSection());

        cardRequest = "SB: 2x Counterspelling TMP";  // adding deck section reference
        unknonwCardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(unknonwCardToken);
        assertEquals(unknonwCardToken.getType(), TokenType.UNKNOWN_CARD);
        assertNull(unknonwCardToken.getCard());
        assertNull(unknonwCardToken.getTokenSection());

        cardRequest = "SB: 2x Counterspelling TMP (F)";  // adding deck section reference
        unknonwCardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(unknonwCardToken);
        assertEquals(unknonwCardToken.getType(), TokenType.UNKNOWN_CARD);
        assertNull(unknonwCardToken.getCard());
        assertNull(unknonwCardToken.getTokenSection());

        cardRequest = "SB: 2x Counterspelling+ TMP";  // adding deck section reference
        unknonwCardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(unknonwCardToken);
        assertEquals(unknonwCardToken.getType(), TokenType.UNKNOWN_CARD);
        assertNull(unknonwCardToken.getCard());
        assertNull(unknonwCardToken.getTokenSection());
    }

    /*===============
     * TEST TOKEN-KEY
     * ============== */
    @Test void testTokenKeyForLegalCard() {
        DeckRecognizer recognizer = new DeckRecognizer();
        String cardRequest = "Viashino Sandstalker";
        Token cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Viashino Sandstalker");
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(cardToken.getCard().getEdition(), "MB1");

        // Token Key
        Token.TokenKey tokenKey = cardToken.getKey();
        assertNotNull(tokenKey);
        assertEquals(tokenKey.cardName, cardToken.getCard().getName());
        assertEquals(tokenKey.collectorNumber, cardToken.getCard().getCollectorNumber());
        assertEquals(tokenKey.setCode, cardToken.getCard().getEdition());
        assertEquals(tokenKey.tokenType, cardToken.getType());
        assertNotNull(tokenKey.deckSection);
        assertEquals(tokenKey.deckSection, cardToken.getTokenSection());
        assertNull(tokenKey.limitedType);
    }

    @Test void testTokenKeyWithGameConstraints(){
        DeckRecognizer recognizer = new DeckRecognizer();
        List<String> allowedSetCodes = Arrays.asList("MIR", "VIS", "WTH");
        List<String> bannedCards = Collections.singletonList("Squandered Resources");
        List<String> restrictedCards = Collections.singletonList("Viashino Sandstalker");

        assertEquals(StaticData.instance().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        // == RESTRICTED CARD
        recognizer.setGameFormatConstraint(allowedSetCodes, bannedCards, restrictedCards);

        String cardRequest = "2x Viashino Sandstalker";
        Token cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LIMITED_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getLimitedCardType());
        assertEquals(cardToken.getLimitedCardType(), DeckRecognizer.LimitedCardType.RESTRICTED);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Viashino Sandstalker");
        assertEquals(cardToken.getQuantity(), 2);
        assertEquals(cardToken.getCard().getEdition(), "VIS");

        // Token Key
        Token.TokenKey tokenKey = cardToken.getKey();
        assertNotNull(tokenKey);
        assertEquals(tokenKey.cardName, cardToken.getCard().getName());
        assertEquals(tokenKey.collectorNumber, cardToken.getCard().getCollectorNumber());
        assertEquals(tokenKey.setCode, cardToken.getCard().getEdition());
        assertEquals(tokenKey.tokenType, cardToken.getType());
        assertNotNull(tokenKey.deckSection);
        assertEquals(tokenKey.deckSection, cardToken.getTokenSection());
        assertNotNull(tokenKey.limitedType);
        assertEquals(tokenKey.limitedType, cardToken.getLimitedCardType());

        // BANNED Card
        cardRequest = "Squandered Resources";
        cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LIMITED_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getLimitedCardType());
        assertEquals(cardToken.getLimitedCardType(), DeckRecognizer.LimitedCardType.BANNED);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Squandered Resources");
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(cardToken.getCard().getEdition(), "VIS");

        // Token Key
        tokenKey = cardToken.getKey();
        assertNotNull(tokenKey);
        assertEquals(tokenKey.cardName, cardToken.getCard().getName());
        assertEquals(tokenKey.collectorNumber, cardToken.getCard().getCollectorNumber());
        assertEquals(tokenKey.setCode, cardToken.getCard().getEdition());
        assertEquals(tokenKey.tokenType, cardToken.getType());
        assertNotNull(tokenKey.deckSection);
        assertEquals(tokenKey.deckSection, cardToken.getTokenSection());
        assertNotNull(tokenKey.limitedType);
        assertEquals(tokenKey.limitedType, cardToken.getLimitedCardType());
    }

    @Test void testTokenKeyWithNonCardTokens(){
        DeckRecognizer recognizer = new DeckRecognizer();
        // Deck Name
        String line = "Name: Test Deck";
        Token lineToken = recognizer.recognizeLine(line, null);
        assertNotNull(lineToken);
        assertEquals(lineToken.getType(), TokenType.DECK_NAME);
        assertFalse(lineToken.isCardToken());
        assertTrue(lineToken.isTokenForDeck());
        assertNull(lineToken.getCard());
        assertNull(lineToken.getKey());
        assertNull(lineToken.getTokenSection());
        assertNull(lineToken.getLimitedCardType());

        // Comment
        line = "// THIS IS A COMMENT";
        lineToken = recognizer.recognizeLine(line, null);
        assertNotNull(lineToken);
        assertEquals(lineToken.getType(), TokenType.COMMENT);
        assertFalse(lineToken.isCardToken());
        assertFalse(lineToken.isTokenForDeck());
        assertNull(lineToken.getCard());
        assertNull(lineToken.getKey());
        assertNull(lineToken.getTokenSection());
        assertNull(lineToken.getLimitedCardType());

        // Unknown Text
        line = "Unknown Text to be ignored";
        lineToken = recognizer.recognizeLine(line, null);
        assertNotNull(lineToken);
        assertEquals(lineToken.getType(), TokenType.UNKNOWN_TEXT);
        assertFalse(lineToken.isCardToken());
        assertFalse(lineToken.isTokenForDeck());
        assertNull(lineToken.getCard());
        assertNull(lineToken.getKey());
        assertNull(lineToken.getTokenSection());
        assertNull(lineToken.getLimitedCardType());

        // Deck Section
        line = "Main";
        lineToken = recognizer.recognizeLine(line, null);
        assertNotNull(lineToken);
        assertEquals(lineToken.getType(), TokenType.DECK_SECTION_NAME);
        assertFalse(lineToken.isCardToken());
        assertFalse(lineToken.isTokenForDeck());
        assertNull(lineToken.getCard());
        assertNull(lineToken.getKey());
        assertNull(lineToken.getTokenSection());
        assertNull(lineToken.getLimitedCardType());

        // Colour Token
        line = "Black";
        lineToken = recognizer.recognizeLine(line, null);
        assertNotNull(lineToken);
        assertEquals(lineToken.getType(), TokenType.MANA_COLOUR);
        assertFalse(lineToken.isCardToken());
        assertFalse(lineToken.isTokenForDeck());
        assertNull(lineToken.getCard());
        assertNull(lineToken.getKey());
        assertNull(lineToken.getTokenSection());
        assertNull(lineToken.getLimitedCardType());

        // Unknown Card Request
        line = "2x Counterspell FEM";
        lineToken = recognizer.recognizeLine(line, null);
        assertNotNull(lineToken);
        assertEquals(lineToken.getType(), TokenType.UNKNOWN_CARD);
        assertFalse(lineToken.isCardToken());
        assertFalse(lineToken.isTokenForDeck());
        assertNull(lineToken.getCard());
        assertNull(lineToken.getKey());
        assertNull(lineToken.getTokenSection());
        assertNull(lineToken.getLimitedCardType());

        // Invalid Card Request
        recognizer = new DeckRecognizer();
        recognizer.setDateConstraint(1994, 2);

        line = "Viashino Sandstalker|VIS";
        lineToken = recognizer.recognizeLine(line, null);
        assertNotNull(lineToken);
        assertEquals(lineToken.getType(), TokenType.CARD_FROM_INVALID_SET);
        assertTrue(lineToken.isCardToken());
        assertNotNull(lineToken.getCard());
        assertFalse(lineToken.isTokenForDeck());
        assertNotNull(lineToken.getKey());
        Token.TokenKey tokenKey = lineToken.getKey();
        assertEquals(tokenKey.cardName, lineToken.getCard().getName());
        assertEquals(tokenKey.setCode, lineToken.getCard().getEdition());
        assertEquals(tokenKey.collectorNumber, lineToken.getCard().getCollectorNumber());
        assertEquals(tokenKey.tokenType, lineToken.getType());
        assertNull(lineToken.getTokenSection());
        assertNull(lineToken.getLimitedCardType());

        // Card from not-allowed set
        recognizer = new DeckRecognizer();
        recognizer.setGameFormatConstraint(Arrays.asList("MIR", "VIS"), null, null);

        line = "Viashino Sandstalker|MB1";
        lineToken = recognizer.recognizeLine(line, null);
        assertNotNull(lineToken);
        assertEquals(lineToken.getType(), TokenType.CARD_FROM_NOT_ALLOWED_SET);
        assertTrue(lineToken.isCardToken());
        assertNotNull(lineToken.getCard());
        assertFalse(lineToken.isTokenForDeck());
        tokenKey = lineToken.getKey();
        assertEquals(tokenKey.cardName, lineToken.getCard().getName());
        assertEquals(tokenKey.setCode, lineToken.getCard().getEdition());
        assertEquals(tokenKey.collectorNumber, lineToken.getCard().getCollectorNumber());
        assertEquals(tokenKey.tokenType, lineToken.getType());
        assertNull(lineToken.getTokenSection());
        assertNull(lineToken.getLimitedCardType());
    }

    @Test void testTokenKeyFromString(){
        DeckRecognizer recognizer = new DeckRecognizer();
        String cardRequest = "Viashino Sandstalker";
        Token cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Viashino Sandstalker");
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(cardToken.getCard().getEdition(), "MB1");

        // Token Key
        Token.TokenKey tokenKey = cardToken.getKey();
        assertNotNull(tokenKey);
        assertEquals(tokenKey.cardName, cardToken.getCard().getName());
        assertEquals(tokenKey.collectorNumber, cardToken.getCard().getCollectorNumber());
        assertEquals(tokenKey.setCode, cardToken.getCard().getEdition());
        assertEquals(tokenKey.tokenType, cardToken.getType());
        assertNotNull(tokenKey.deckSection);
        assertEquals(tokenKey.deckSection, cardToken.getTokenSection());
        assertNull(tokenKey.limitedType);

        // Create Token String representation
        String tokenString = tokenKey.toString();
        Token.TokenKey newTokenKey = Token.TokenKey.fromString(tokenString);
        assertNotNull(newTokenKey);
        assertEquals(newTokenKey.cardName, tokenKey.cardName);
        assertEquals(newTokenKey.collectorNumber, tokenKey.collectorNumber);
        assertEquals(newTokenKey.setCode, tokenKey.setCode);
        assertEquals(newTokenKey.tokenType, tokenKey.tokenType);
        assertEquals(newTokenKey.deckSection, tokenKey.deckSection);
        assertEquals(newTokenKey.limitedType, tokenKey.limitedType);
    }

    @Test void testTokenKeyWithFoiledCard(){
        DeckRecognizer recognizer = new DeckRecognizer();
        String cardRequest = "Mountain|M21 (F)";
        Token cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Mountain");
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(cardToken.getCard().getEdition(), "M21");
        assertTrue(cardToken.getCard().isFoil());

        // Token Key
        Token.TokenKey tokenKey = cardToken.getKey();
        assertNotNull(tokenKey);
        String expectedKeyCardName = CardDb.CardRequest.compose(cardToken.getCard().getName(),
                cardToken.getCard().isFoil());
        assertEquals(tokenKey.cardName, expectedKeyCardName);
        assertTrue(tokenKey.cardName.endsWith(CardDb.foilSuffix));
        assertEquals(tokenKey.collectorNumber, cardToken.getCard().getCollectorNumber());
        assertEquals(tokenKey.setCode, cardToken.getCard().getEdition());
        assertEquals(tokenKey.tokenType, cardToken.getType());
        assertNotNull(tokenKey.deckSection);
        assertEquals(tokenKey.deckSection, cardToken.getTokenSection());
    }

    @Test void testTokenKeyFoilCardFromString() {
        DeckRecognizer recognizer = new DeckRecognizer();
        String cardRequest = "Mountain|M21 (F)";
        Token cardToken = recognizer.recogniseCardToken(cardRequest, null);
        assertNotNull(cardToken);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getCard().getName(), "Mountain");
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(cardToken.getCard().getEdition(), "M21");
        assertTrue(cardToken.getCard().isFoil());

        // Token Key
        Token.TokenKey tokenKey = cardToken.getKey();
        assertNotNull(tokenKey);
        String expectedKeyCardName = CardDb.CardRequest.compose(cardToken.getCard().getName(),
                cardToken.getCard().isFoil());
        assertEquals(tokenKey.cardName, expectedKeyCardName);
        assertTrue(tokenKey.cardName.endsWith(CardDb.foilSuffix));
        assertEquals(tokenKey.collectorNumber, cardToken.getCard().getCollectorNumber());
        assertEquals(tokenKey.setCode, cardToken.getCard().getEdition());
        assertEquals(tokenKey.tokenType, cardToken.getType());
        assertNotNull(tokenKey.deckSection);
        assertEquals(tokenKey.deckSection, cardToken.getTokenSection());

        // Create Token String representation
        String tokenString = tokenKey.toString();
        Token.TokenKey newTokenKey = Token.TokenKey.fromString(tokenString);
        assertNotNull(newTokenKey);
        assertEquals(newTokenKey.cardName, tokenKey.cardName);
        assertEquals(newTokenKey.collectorNumber, tokenKey.collectorNumber);
        assertEquals(newTokenKey.setCode, tokenKey.setCode);
        assertEquals(newTokenKey.tokenType, tokenKey.tokenType);
        assertEquals(newTokenKey.deckSection, tokenKey.deckSection);
        assertEquals(newTokenKey.limitedType, tokenKey.limitedType);
    }

    /*====================================
     * TEST PARSE INPUT
     * ==================================*/

    // === MIXED inputs ===
    @Test void testRecognizeLines(){
        DeckRecognizer recognizer = new DeckRecognizer();

        assertEquals(StaticData.instance().getCommonCards().getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String lineRequest = "// MainBoard";
        Token token = recognizer.recognizeLine(lineRequest, null);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.DECK_SECTION_NAME);
        assertEquals(token.getText(), "Main");

        lineRequest = "## Sideboard (15)";
        token = recognizer.recognizeLine(lineRequest, null);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.DECK_SECTION_NAME);
        assertEquals(token.getText(), "Sideboard");

        lineRequest = "Normal Text";
        token = recognizer.recognizeLine(lineRequest, null);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.UNKNOWN_TEXT);
        assertEquals(token.getText(), "Normal Text");

        lineRequest = "//Creatures";
        token = recognizer.recognizeLine(lineRequest, null);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.CARD_TYPE);
        assertEquals(token.getText(), "Creatures");

        lineRequest = "//Lands";
        token = recognizer.recognizeLine(lineRequest, null);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.CARD_TYPE);
        assertEquals(token.getText(), "Lands");

        lineRequest = "//Land";
        token = recognizer.recognizeLine(lineRequest, null);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.CARD_RARITY);
        assertEquals(token.getText(), "Land");

        lineRequest = "//Creatures with text";
        token = recognizer.recognizeLine(lineRequest, null);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.COMMENT);
        assertEquals(token.getText(), "//Creatures with text");

        lineRequest = "SB: Ancestral Recall";
        token = recognizer.recognizeLine(lineRequest, null);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.LEGAL_CARD);
        assertEquals(token.getText(), "Ancestral Recall [VMA] #1");
        assertNotNull(token.getCard());
        assertNotNull(token.getTokenSection());
        assertEquals(token.getTokenSection(), DeckSection.Sideboard);
        assertTrue(token.cardRequestHasNoCode());

        lineRequest = "SB:Ancestral Recall";
        token = recognizer.recognizeLine(lineRequest, null);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.LEGAL_CARD);
        assertEquals(token.getText(), "Ancestral Recall [VMA] #1");
        assertNotNull(token.getCard());
        assertNotNull(token.getTokenSection());
        assertEquals(token.getTokenSection(), DeckSection.Sideboard);
        assertTrue(token.cardRequestHasNoCode());

        lineRequest = "Ancestral Recall";
        token = recognizer.recognizeLine(lineRequest, null);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.LEGAL_CARD);
        assertEquals(token.getText(), "Ancestral Recall [VMA] #1");
        assertNotNull(token.getCard());
        assertTrue(token.cardRequestHasNoCode());

        lineRequest = "* 4 [Counterspell](http://tappedout.nethttp://tappedout.net/mtg-card/counterspell/)";
        token = recognizer.recognizeLine(lineRequest, null);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.LEGAL_CARD);
        assertNotNull(token.getText());
        assertNotNull(token.getCard());
        assertEquals(token.getQuantity(), 4);
        assertTrue(token.cardRequestHasNoCode());

        lineRequest = "### Instant (14)";
        token = recognizer.recognizeLine(lineRequest, null);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.CARD_TYPE);
        assertEquals(token.getText(), "Instant");

        lineRequest = "### General line as comment";
        token = recognizer.recognizeLine(lineRequest, null);
        assertNotNull(token);
        assertEquals(token.getType(), TokenType.COMMENT);
        assertEquals(token.getText(), "### General line as comment");
    }

    // === Parsing Card List ===
    @Test void testParsingCardListNoConstraint(){
        String[] cardList = new String[]{
                "//Sideboard",  // decksection
                "2x Counterspell FEM", // unknonw card
                "4x Incinerate|ICE",  // known card to side
                "Gibberish to ignore",  // ignored
                "#Comment to report"   // comment token
        };
        DeckRecognizer recognizer = new DeckRecognizer();
        List<Token> tokens = recognizer.parseCardList(cardList);
        assertNotNull(tokens);
        assertEquals(tokens.size(), 5);

        Token deckSectionToken = tokens.get(0);
        assertEquals(deckSectionToken.getType(), TokenType.DECK_SECTION_NAME);
        assertNull(deckSectionToken.getTokenSection());
        assertEquals(deckSectionToken.getText(), DeckSection.Sideboard.name());

        Token unknownCardToken = tokens.get(1);
        assertEquals(unknownCardToken.getType(), TokenType.UNKNOWN_CARD);
        Token unknownText = tokens.get(3);
        assertEquals(unknownText.getType(), TokenType.UNKNOWN_TEXT);

        Token cardToken = tokens.get(2);
        assertTrue(cardToken.isCardToken());
        assertNotNull(cardToken.getCard());
        assertFalse(cardToken.cardRequestHasNoCode());
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(cardToken.getCard().getName(), "Incinerate");
        assertEquals(cardToken.getCard().getEdition(), "ICE");
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        // This is because of the previous DeckSection token
        assertEquals(cardToken.getTokenSection(), DeckSection.Sideboard);

        Token commentToken = tokens.get(4);
        assertEquals(commentToken.getType(), TokenType.COMMENT);
    }

    @Test void testParseCardListWithAllowedSectionsRaisesUnsupportedCardsAndSection(){
        String[] cardList = new String[]{
                "//Sideboard",  // decksection - unsupported section
                "All in Good Time", // Schemes - unsupported card
                "4x Incinerate|ICE",  // known card to main
        };
        DeckRecognizer recognizer = new DeckRecognizer();
        recognizer.setAllowedDeckSections(Arrays.asList(DeckSection.Main, DeckSection.Commander));
        List<Token> tokens = recognizer.parseCardList(cardList);
        assertNotNull(tokens);
        assertEquals(tokens.size(), 4);

        Token deckSectionToken = tokens.get(0);
        assertFalse(deckSectionToken.isDeckSection());
        assertEquals(deckSectionToken.getType(), TokenType.UNSUPPORTED_DECK_SECTION);
        assertNull(deckSectionToken.getTokenSection());

        Token unsupportedCard = tokens.get(1);
        assertEquals(unsupportedCard.getType(), TokenType.UNSUPPORTED_CARD);

        deckSectionToken = tokens.get(2);
        assertTrue(deckSectionToken.isDeckSection());
        assertEquals(deckSectionToken.getText(), DeckSection.Main.name());

        Token cardToken = tokens.get(3);
        assertTrue(cardToken.isCardToken());
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(cardToken.getCard().getName(), "Incinerate");
        assertEquals(cardToken.getCard().getEdition(), "ICE");
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
    }

    @Test void testCardListWithDeckNameHasDeckNameOnTop(){
        String[] cardList = new String[]{
                "//Sideboard",  // decksection
                "Name: Test deck", // goes on top
                "4x Incinerate|ICE",  // known card to side
        };
        DeckRecognizer recognizer = new DeckRecognizer();
        List<Token> tokens = recognizer.parseCardList(cardList);
        assertNotNull(tokens);
        assertEquals(tokens.size(), 3);

        Token deckNameToken = tokens.get(0);
        assertEquals(deckNameToken.getType(), TokenType.DECK_NAME);
        assertEquals(deckNameToken.getText(), "Test deck");

        Token deckSectionToken = tokens.get(1);
        assertTrue(deckSectionToken.isDeckSection());
        assertEquals(deckSectionToken.getType(), TokenType.DECK_SECTION_NAME);
        assertEquals(deckSectionToken.getText(), DeckSection.Sideboard.name());

        Token cardToken = tokens.get(2);
        assertTrue(cardToken.isCardToken());
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(cardToken.getCard().getName(), "Incinerate");
        assertEquals(cardToken.getCard().getEdition(), "ICE");
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Sideboard);
    }

    @Test void testCardsInDifferentSectionsWillAddAlsoDeckSectionPlaceholders(){
        String[] cardList = new String[]{
                "2x Counterspell | TMP",  // card legal in Main (+placeholder)
                "SB:4x Incinerate|ICE",  // card legal in Side (+ placeholder)
                "2x Fireball 5ED"  // card legal in Side
        };
        DeckRecognizer recognizer = new DeckRecognizer();
        List<Token> tokens = recognizer.parseCardList(cardList);
        assertNotNull(tokens);
        assertEquals(tokens.size(), 5);

        Token deckSectionToken = tokens.get(0);
        assertEquals(deckSectionToken.getType(), TokenType.DECK_SECTION_NAME);
        assertTrue(deckSectionToken.isDeckSection());
        assertEquals(deckSectionToken.getText(), DeckSection.Main.name());

        Token cardToken = tokens.get(1);
        assertTrue(cardToken.isCardToken());
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 2);
        assertEquals(cardToken.getCard().getName(), "Counterspell");
        assertEquals(cardToken.getCard().getEdition(), "TMP");
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);

        deckSectionToken = tokens.get(2);
        assertEquals(deckSectionToken.getType(), TokenType.DECK_SECTION_NAME);
        assertTrue(deckSectionToken.isDeckSection());
        assertEquals(deckSectionToken.getText(), DeckSection.Sideboard.name());

        cardToken = tokens.get(3);
        assertTrue(cardToken.isCardToken());
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 4);
        assertEquals(cardToken.getCard().getName(), "Incinerate");
        assertEquals(cardToken.getCard().getEdition(), "ICE");
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Sideboard);

        cardToken = tokens.get(4);
        assertTrue(cardToken.isCardToken());
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 2);
        assertEquals(cardToken.getCard().getName(), "Fireball");
        assertEquals(cardToken.getCard().getEdition(), "5ED");
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Sideboard);
    }

    @Test void testDeckSectionValidationForTokensAddPlaceholderAndRestoresMainSection(){
        String[] cardList = new String[]{
                "2x Counterspell | TMP",  // card legal in Main (+placeholder)
                "All in Good Time",  // card legal in Schemes (+ placeholder)
                "2x Fireball 5ED"  // card legal in Main (+ placeholder as section changes again)
        };
        DeckRecognizer recognizer = new DeckRecognizer();
        List<Token> tokens = recognizer.parseCardList(cardList);
        assertNotNull(tokens);
        assertEquals(tokens.size(), 6);

        Token deckSectionToken = tokens.get(0);
        assertEquals(deckSectionToken.getType(), TokenType.DECK_SECTION_NAME);
        assertTrue(deckSectionToken.isDeckSection());
        assertEquals(deckSectionToken.getText(), DeckSection.Main.name());

        Token cardToken = tokens.get(1);
        assertTrue(cardToken.isCardToken());
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 2);
        assertEquals(cardToken.getCard().getName(), "Counterspell");
        assertEquals(cardToken.getCard().getEdition(), "TMP");
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);

        deckSectionToken = tokens.get(2);
        assertEquals(deckSectionToken.getType(), TokenType.DECK_SECTION_NAME);
        assertTrue(deckSectionToken.isDeckSection());
        assertEquals(deckSectionToken.getText(), DeckSection.Schemes.name());

        cardToken = tokens.get(3);
        assertTrue(cardToken.isCardToken());
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 1);
        assertEquals(cardToken.getCard().getName(), "All in Good Time");
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Schemes);

        deckSectionToken = tokens.get(4);
        assertEquals(deckSectionToken.getType(), TokenType.DECK_SECTION_NAME);
        assertTrue(deckSectionToken.isDeckSection());
        assertEquals(deckSectionToken.getText(), DeckSection.Main.name());

        cardToken = tokens.get(5);
        assertTrue(cardToken.isCardToken());
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getQuantity(), 2);
        assertEquals(cardToken.getCard().getName(), "Fireball");
        assertEquals(cardToken.getCard().getEdition(), "5ED");
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);
    }

    @Test void testUnsupportedCardIsReturnedOnlyWhenNoOtherOptionExistForSectionMatching(){
        DeckRecognizer recognizer = new DeckRecognizer();
        // Now add constraint
        recognizer.setAllowedDeckSections(Arrays.asList(DeckSection.Main, DeckSection.Sideboard));

        String[] cardList = new String[]{
                "1 Anowon, the Ruin Thief", // Commander Section but will go in MAIN + placeholder
                "All in Good Time",  // card legal in Schemes but Section not allowed - UNSUPPORTED
        };

        List<Token> parsedTokens = recognizer.parseCardList(cardList);
        assertEquals(parsedTokens.size(), 3);

        Token deckSection = parsedTokens.get(0);
        assertTrue(deckSection.isDeckSection());
        assertEquals(deckSection.getType(), TokenType.DECK_SECTION_NAME);
        assertEquals(deckSection.getText(), DeckSection.Main.name());

        Token cardToken = parsedTokens.get(1);
        assertEquals(cardToken.getType(), TokenType.LEGAL_CARD);
        assertNotNull(cardToken.getCard());
        assertEquals(cardToken.getTokenSection(), DeckSection.Main);

        Token unsupportedCard = parsedTokens.get(2);
        assertEquals(unsupportedCard.getType(), TokenType.UNSUPPORTED_CARD);
    }

}
