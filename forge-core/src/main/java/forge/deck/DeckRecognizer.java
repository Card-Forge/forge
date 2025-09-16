/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.deck;

import com.google.common.collect.Lists;
import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardType;
import forge.card.MagicColor;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.util.Localizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * DeckRecognizer class.
 * </p>
 * 
 */
public class DeckRecognizer {
    /**
     * The Enum TokenType.
     */
    public enum TokenType {
        // Card Token Types
        LEGAL_CARD,
        LIMITED_CARD,
        CARD_FROM_NOT_ALLOWED_SET,
        CARD_FROM_INVALID_SET,
        /**
         * Valid card request, but can't be imported because the player does not have enough copies.
         * Should be replaced with a different printing if possible.
         */
        CARD_NOT_IN_INVENTORY,
        /**
         * Valid card request for a card that isn't in the player's inventory, but new copies can be acquired freely.
         * Usually used for basic lands. Should be supplied to the import controller by the editor.
         */
        FREE_CARD_NOT_IN_INVENTORY,
        // Warning messages
        WARNING_MESSAGE,
        UNKNOWN_CARD,
        UNSUPPORTED_CARD,
        UNSUPPORTED_DECK_SECTION,
        // No Token
        UNKNOWN_TEXT,
        COMMENT,
        // Placeholders
        DECK_NAME,
        DECK_SECTION_NAME,
        CARD_TYPE,
        CARD_RARITY,
        CARD_CMC,
        MANA_COLOUR;

        public static final EnumSet<TokenType> CARD_TOKEN_TYPES = EnumSet.of(LEGAL_CARD, LIMITED_CARD, CARD_FROM_NOT_ALLOWED_SET, CARD_FROM_INVALID_SET, CARD_NOT_IN_INVENTORY, FREE_CARD_NOT_IN_INVENTORY);
        public static final EnumSet<TokenType> IN_DECK_TOKEN_TYPES = EnumSet.of(LEGAL_CARD, LIMITED_CARD, DECK_NAME, FREE_CARD_NOT_IN_INVENTORY);
        public static final EnumSet<TokenType> CARD_PLACEHOLDER_TOKEN_TYPES = EnumSet.of(CARD_TYPE, CARD_RARITY, CARD_CMC, MANA_COLOUR);
    }

    public enum LimitedCardType {
        BANNED,
        RESTRICTED,
    }

    /**
     * The Class Token.
     */
    public static class Token {
        private final TokenType type;
        private final int number;
        private final String text;
        // only used for illegal card tokens
        private LimitedCardType limitedCardType = null;
        // only used for card tokens
        private PaperCard card = null;
        private DeckSection tokenSection = null;
        // Flag used to mark whether original card request had any specified set code
        // This will be used to mark tokens that could be further processed by
        // card art optimisation (if enabled)
        private boolean cardRequestHasSetCode = true;


        public static Token LegalCard(final PaperCard card, final int count,
                                      final DeckSection section, final boolean cardRequestHasSetCode) {
            return new Token(TokenType.LEGAL_CARD, count, card, section, cardRequestHasSetCode);
        }

        public static Token LimitedCard(final PaperCard card, final int count,
                                        final DeckSection section, final LimitedCardType limitedType,
                                        final boolean cardRequestHasSetCode){
            return new Token(TokenType.LIMITED_CARD, count, card, section, limitedType, cardRequestHasSetCode);
        }

        public static Token NotAllowedCard(final PaperCard card, final int count, final boolean cardRequestHasSetCode) {
            return new Token(TokenType.CARD_FROM_NOT_ALLOWED_SET, count, card, cardRequestHasSetCode);
        }

        public static Token CardInInvalidSet(final PaperCard card, final int count, final boolean cardRequestHasSetCode) {
            return new Token(TokenType.CARD_FROM_INVALID_SET, count, card, cardRequestHasSetCode);
        }

        public static Token NotInInventoryFree(final PaperCard card, final int count, final DeckSection section) {
            return new Token(TokenType.FREE_CARD_NOT_IN_INVENTORY, count, card, section, true);
        }

        // WARNING MESSAGES
        // ================
        public static Token UnknownCard(final String cardName, final String setCode, final int count) {
            String ttext = setCode == null || setCode.isEmpty() ? cardName :
                    String.format("%s [%s]", cardName, setCode);
            return new Token(TokenType.UNKNOWN_CARD, count, ttext);
        }

        public static Token UnsupportedCard(final String cardName, final String setCode, final int count) {
            String ttext = setCode == null || setCode.isEmpty() ? cardName :
                    String.format("%s [%s]", cardName, setCode);
            return new Token(TokenType.UNSUPPORTED_CARD, count, ttext);
        }

        public static Token WarningMessage(String msg) {
           return new Token(TokenType.WARNING_MESSAGE, msg);
        }

        public static Token NotInInventory(final PaperCard card, final int count, final DeckSection section) {
            return new Token(TokenType.CARD_NOT_IN_INVENTORY, count, card, section, false);
        }

        /* =================================
         * DECK SECTIONS
         * ================================= */
        private static Token UnsupportedDeckSection(final String sectionName){
            return new Token(TokenType.UNSUPPORTED_DECK_SECTION, sectionName);
        }

        public static Token DeckSection(final String sectionName0, List<DeckSection> allowedDeckSections){
            String sectionName = sectionName0.toLowerCase().trim();
            DeckSection matchedSection = null;
            if (sectionName.equals("side") || sectionName.contains("sideboard") || sectionName.equals("sb"))
                matchedSection = DeckSection.Sideboard;
            else if (sectionName.equals("main") || sectionName.contains("card")
                    || sectionName.equals("mainboard") || sectionName.equals("deck"))
                matchedSection = DeckSection.Main;
            else if (sectionName.equals("avatar"))
                matchedSection = DeckSection.Avatar;
            else if (sectionName.equals("commander"))
                matchedSection = DeckSection.Commander;
            else if (sectionName.equals("schemes"))
                matchedSection = DeckSection.Schemes;
            else if (sectionName.equals("conspiracy"))
                matchedSection = DeckSection.Conspiracy;
            else if (sectionName.equals("planes"))
                matchedSection = DeckSection.Planes;
            else if (sectionName.equals("attractions"))
                matchedSection = DeckSection.Attractions;
            else if (sectionName.equals("contraptions"))
                matchedSection = DeckSection.Contraptions;

            if (matchedSection == null)  // no match found
                return null;

            if (allowedDeckSections != null && !allowedDeckSections.contains(matchedSection))
                return Token.UnsupportedDeckSection(sectionName0);
            return new Token(TokenType.DECK_SECTION_NAME, matchedSection.name());
        }

        private Token(final TokenType type1, final int count, final PaperCard tokenCard, boolean cardRequestHasSetCode) {
            this.number = count;
            this.type = type1;
            this.text = "";
            this.card = tokenCard;
            this.tokenSection = null;
            this.limitedCardType = null;
            this.cardRequestHasSetCode = cardRequestHasSetCode;
        }

        private Token(final TokenType type1, final int count, final PaperCard tokenCard,
                      final DeckSection section, boolean cardRequestHasSetCode) {
            this(type1, count, tokenCard, cardRequestHasSetCode);
            this.tokenSection = section;
            this.limitedCardType = null;
        }

        private Token(final TokenType type1, final int count, final PaperCard tokenCard,
                      final DeckSection section, final LimitedCardType limitedCardType1,
                      boolean cardRequestHasSetCode) {
            this(type1, count, tokenCard, cardRequestHasSetCode);
            this.tokenSection = section;
            this.limitedCardType = limitedCardType1;
        }

        public Token(final TokenType type1, final int count, final String message) {
            this.number = count;
            this.type = type1;
            this.text = message;
        }

        public Token(final TokenType type1, final String message) {
            this(type1, 0, message);
        }

        public final String getText() {
            if (this.isCardToken())
                return String.format("%s [%s] #%s",
                        this.card.getName(), this.card.getEdition(), this.card.getCollectorNumber());
            return this.text;
        }

        public final PaperCard getCard() {
            return this.card;
        }

        public final TokenType getType() {
            return this.type;
        }

        public final int getQuantity() {
            return this.number;
        }

        public final boolean cardRequestHasNoCode() {
            return !(this.cardRequestHasSetCode);
        }

        public final DeckSection getTokenSection() { return this.tokenSection; }

        public void resetTokenSection(DeckSection referenceDeckSection) {
            this.tokenSection = referenceDeckSection != null ? referenceDeckSection : DeckSection.Main;
        }

        public void replaceTokenCard(PaperCard replacementCard){
            if (!this.isCardToken())
                return;
            this.card = replacementCard;
        }

        public final LimitedCardType getLimitedCardType() { return this.limitedCardType; }

        /**
         * Filters all token types that have a PaperCard instance set (not null)
         * @return true for tokens of type:
         * LEGAL_CARD, LIMITED_CARD, CARD_FROM_NOT_ALLOWED_SET and CARD_FROM_INVALID_SET, CARD_NOT_IN_INVENTORY, FREE_CARD_NOT_IN_INVENTORY.
         * False otherwise.
         */
        public boolean isCardToken() {
            return TokenType.CARD_TOKEN_TYPES.contains(this.type);
        }

        /**
         * Filters all tokens that will be potentially considered during Deck Import.
         * @return true if the type of the token is one of:
         * LEGAL_CARD, LIMITED_CARD, DECK_NAME; false otherwise.
         */
        public boolean isTokenForDeck() {
            return TokenType.IN_DECK_TOKEN_TYPES.contains(this.type);
        }

        /**
         * Filters all tokens for deck that are also Card Token..
         * @return true for tokens of type: LEGAL_CARD, LIMITED_CARD.
         * False otherwise.
         */
        public boolean isCardTokenForDeck() {
            return isCardToken() && isTokenForDeck();
        }

        /**
         * Determines whether current token is a placeholder token for card categories,
         * only used for Decklist formatting.
         * @return true if the type of the token is one of:
         * CARD_RARITY, CARD_CMC, CARD_TYPE, MANA_COLOUR
         */
        public boolean isCardPlaceholder(){
            return TokenType.CARD_PLACEHOLDER_TOKEN_TYPES.contains(this.type);
        }

        /** Determines if current token is a Deck Section token
         * @return true if the type of token is DECK_SECTION_NAMES
         */
        public boolean isDeckSection(){ return this.type == TokenType.DECK_SECTION_NAME; }

        /**
         * Generates the key for the current token, which is a hyphenated string including
         * "Card Name", "Card Edition", "Card's Collector Number", "token-type", and
         * the "token section" (if any).
         * @return null if the current token, is a non-card token, else an instance of TokeKey
         * data object will be returned.
         * @see Token#isCardToken()
         * @see Token.TokenKey#fromToken(Token)
         */
        public TokenKey getKey(){
            return TokenKey.fromToken(this);
        }

        /**
         * Encapsulate the logic for a Token Key (Data Object)
         */
        public static class TokenKey {
            private static final String KEYSEP = "|";

            public String cardName;
            public String setCode;
            public String collectorNumber;
            public DeckSection deckSection;
            public TokenType tokenType;
            public LimitedCardType limitedType;

            /**
             * Instantiate a new TokeKey for the given card token token.
             * @param token Input token to generate the key for.
             * @return null if input token is not a CardToken
             * @see Token#isCardToken()
             */
            public static TokenKey fromToken(final Token token){
                if (!token.isCardToken())
                    return null;
                TokenKey key = new TokenKey();
                key.cardName = CardDb.CardRequest.compose(token.card.getName(), token.getCard().isFoil());
                key.setCode = token.card.getEdition();
                key.collectorNumber = token.card.getCollectorNumber();
                key.tokenType = token.getType();
                if (token.tokenSection != null)
                    key.deckSection = token.tokenSection;
                if (token.limitedCardType != null)
                    key.limitedType = token.limitedCardType;
                return key;
            }

            /**
             * String representation of a Token Key (to be used as reference to target token)
             * @return A String (separated by KEYSEP) containing all the token-key attributes.
             * Non-card parts of the keys are identified by an initial capital letter, that is
             * either "D", "T", or "L" to refer to Deck Section (if any), Token Type, and
             * Limited type (if any), respectively.
             */
            public String toString(){
                StringBuilder keyString = new StringBuilder();
                keyString.append(String.format("%s%s%s%s%s", this.cardName, KEYSEP,
                        this.setCode, KEYSEP, this.collectorNumber));
                if (this.deckSection != null)
                    keyString.append(String.format("%sD%s",KEYSEP, this.deckSection.name()));
                keyString.append(String.format("%sT%s", KEYSEP, this.tokenType.name()));
                if (this.limitedType != null)
                    keyString.append(String.format("%sL%s", KEYSEP, this.limitedType.name()));
                return keyString.toString();
            }

            /**
             * Generates a new TokenKey instance starting from a given Key-String
             * @param keyString String representation of a TokenKey
             * @return a new TokenKey object instantiated from the given Key. Null if key string does not
             * non-optional infos, that is "all card info" and "token type".
             */
            public static TokenKey fromString(String keyString){
                String[] keyInfo = StringUtils.split(keyString, KEYSEP);
                if (keyInfo.length < 4)
                    return null;

                TokenKey tokenKey = new TokenKey();
                tokenKey.cardName = keyInfo[0];
                tokenKey.setCode = keyInfo[1];
                tokenKey.collectorNumber = keyInfo[2];
                int nxtInfoIdx = 3;
                if (keyInfo[nxtInfoIdx].startsWith("D")){
                    tokenKey.deckSection = DeckSection.valueOf(keyInfo[nxtInfoIdx].substring(1));
                    nxtInfoIdx += 1;
                }
                TokenType tokenType = TokenType.valueOf(keyInfo[nxtInfoIdx].substring(1));
                tokenKey.tokenType = tokenType;
                if (tokenType == TokenType.LIMITED_CARD)
                    tokenKey.limitedType = LimitedCardType.valueOf(keyInfo[nxtInfoIdx+1].substring(1));
                return tokenKey;
            }
        }
    }

    // Utility Constants
    private static final Pattern SEARCH_SINGLE_SLASH = Pattern.compile("(?<=[^/])\\s*/\\s*(?=[^/])");
    private static final String DOUBLE_SLASH = "//";
    private static final String LINE_COMMENT_DELIMITER_OR_MD_HEADER = "#";
    private static final String ASTERISK = "* ";  // Note the blank space after asterisk!

    // Core Matching Patterns (initialised in Constructor)
    public static final String REGRP_DECKNAME = "deckName";
    public static final String REX_DECK_NAME =
            String.format("^(\\/\\/\\s*)?(?<pre>(deck|name(\\s)?))(\\:|=)\\s*(?<%s>([a-zA-Z0-9',\\/\\-\\s\\)\\]\\(\\[\\#]+))\\s*(.*)$",
                    REGRP_DECKNAME);
    public static final Pattern DECK_NAME_PATTERN = Pattern.compile(REX_DECK_NAME, Pattern.CASE_INSENSITIVE);

    public static final String REGRP_TOKEN = "token";
    public static final String REGRP_COLR1 = "colr1";
    public static final String REGRP_COLR2 = "colr2";
    public static final String REGRP_MANA = "mana";
    public static final String REX_NOCARD = String.format("^(?<pre>[^a-zA-Z]*)\\s*(?<title>(\\w+[:]\\s*))?(?<%s>[a-zA-Z]+)(?<post>[^a-zA-Z]*)?$", REGRP_TOKEN);
    public static final String REX_CMC = String.format("^(?<pre>[^a-zA-Z]*)\\s*(?<%s>(C(M)?C(\\s)?\\d{1,2}))(?<post>[^\\d]*)?$", REGRP_TOKEN);
    public static final String REX_RARITY = String.format("^(?<pre>[^a-zA-Z]*)\\s*(?<%s>((un)?common|(mythic)?\\s*(rare)?|land|special))(?<post>[^a-zA-Z]*)?$", REGRP_TOKEN);
    public static final String MANA_SYMBOLS = "w|u|b|r|g|c|m|wu|ub|br|rg|gw|wb|ur|bg|rw|gu";
    public static final String REX_MANA_SYMBOLS = String.format("\\{(?<%s>(%s))\\}", REGRP_MANA, MANA_SYMBOLS);
    public static final String REX_MANA_COLOURS = String.format("(\\{(%s)\\})|(white|blue|black|red|green|colo(u)?rless|multicolo(u)?r)", MANA_SYMBOLS);
    public static final String REX_MANA = String.format("^(?<pre>[^a-zA-Z]*)\\s*(?<%s>(%s))((\\s|-|\\|)(?<%s>(%s)))?(?<post>[^a-zA-Z]*)?$",
            REGRP_COLR1, REX_MANA_COLOURS, REGRP_COLR2, REX_MANA_COLOURS);
    public static final Pattern NONCARD_PATTERN = Pattern.compile(REX_NOCARD, Pattern.CASE_INSENSITIVE);
    public static final Pattern CMC_PATTERN = Pattern.compile(REX_CMC, Pattern.CASE_INSENSITIVE);
    public static final Pattern CARD_RARITY_PATTERN = Pattern.compile(REX_RARITY, Pattern.CASE_INSENSITIVE);
    public static final Pattern MANA_PATTERN = Pattern.compile(REX_MANA, Pattern.CASE_INSENSITIVE);
    public static final Pattern MANA_SYMBOL_PATTERN = Pattern.compile(REX_MANA_SYMBOLS, Pattern.CASE_INSENSITIVE);

    public static final String REGRP_SET = "setcode";
    public static final String REGRP_COLLNR = "collnr";
    public static final String REGRP_CARD = "cardname";
    public static final String REGRP_CARDNO = "count";

    public static final String REX_CARD_NAME = String.format("(\\[)?(?<%s>[a-zA-Z0-9à-ÿÀ-Ÿ&',\\.:!\\+\\\"\\/\\-\\s]+)(\\])?", REGRP_CARD);
    public static final String REX_SET_CODE = String.format("(?<%s>[a-zA-Z0-9_]{2,7})", REGRP_SET);
    public static final String REX_COLL_NUMBER = String.format("(?<%s>\\*?[0-9A-Z]+\\S?[A-Z]*)", REGRP_COLLNR);
    public static final String REX_CARD_COUNT = String.format("(?<%s>[\\d]{1,2})(?<mult>x)?", REGRP_CARDNO);
    // EXTRA
    public static final String REGRP_FOIL_GFISH = "foil";
    private static final String REX_FOIL_MTGGOLDFISH = String.format(
            "(?<%s>\\(F\\))?", REGRP_FOIL_GFISH);
    // XMage Sideboard indicator - pushed a bit further with deck section indication
    public static final String REGRP_DECK_SEC_XMAGE_STYLE = "decsec";
    private static final String REX_DECKSEC_XMAGE = String.format(
            "(?<%s>(MB|SB|CM))", REGRP_DECK_SEC_XMAGE_STYLE);

    // 1. Card-Set Request (Amount?, CardName, Set)
    public static final String REX_CARD_SET_REQUEST = String.format(
            "(%s\\s*:\\s*)?(%s\\s)?\\s*%s\\s*(\\s|\\||\\(|\\[|\\{)\\s?%s(\\s|\\)|\\]|\\})?\\s*%s",
            REX_DECKSEC_XMAGE, REX_CARD_COUNT, REX_CARD_NAME, REX_SET_CODE, REX_FOIL_MTGGOLDFISH);
    public static final Pattern CARD_SET_PATTERN = Pattern.compile(REX_CARD_SET_REQUEST);
    // 2. Set-Card Request (Amount?, Set, CardName)
    public static final String REX_SET_CARD_REQUEST = String.format(
            "(%s\\s*:\\s*)?(%s\\s)?\\s*(\\(|\\[|\\{)?%s(\\s+|\\)|\\]|\\}|\\|)\\s*%s\\s*%s\\s*",
            REX_DECKSEC_XMAGE, REX_CARD_COUNT, REX_SET_CODE, REX_CARD_NAME, REX_FOIL_MTGGOLDFISH);
    public static final Pattern SET_CARD_PATTERN = Pattern.compile(REX_SET_CARD_REQUEST);
    // 3. Full-Request (Amount?, CardName, Set, Collector Number|Art Index) - MTGArena Format
    public static final String REX_FULL_REQUEST_CARD_SET = String.format(
            "(%s\\s*:\\s*)?(%s\\s)?\\s*%s\\s*(\\||\\(|\\[|\\{|\\s)%s(\\s|\\)|\\]|\\})?(\\s+|\\|\\s*)%s\\s*%s\\s*",
            REX_DECKSEC_XMAGE, REX_CARD_COUNT, REX_CARD_NAME, REX_SET_CODE, REX_COLL_NUMBER, REX_FOIL_MTGGOLDFISH);
    public static final Pattern CARD_SET_COLLNO_PATTERN = Pattern.compile(REX_FULL_REQUEST_CARD_SET);
    // 4. Full-Request (Amount?, Set, CardName, Collector Number|Art Index) - Alternative for flexibility
    public static final String REX_FULL_REQUEST_SET_CARD = String.format(
            "^(%s\\s*:\\s*)?(%s\\s)?\\s*(\\(|\\[|\\{)?%s(\\s+|\\)|\\]|\\}|\\|)\\s*%s(\\s+|\\|\\s*)%s\\s*%s$",
            REX_DECKSEC_XMAGE, REX_CARD_COUNT, REX_SET_CODE, REX_CARD_NAME, REX_COLL_NUMBER, REX_FOIL_MTGGOLDFISH);
    public static final Pattern SET_CARD_COLLNO_PATTERN = Pattern.compile(REX_FULL_REQUEST_SET_CARD);
    // 5. (MTGGoldfish mostly) (Amount?, Card Name, <Collector Number>, Set)
    public static final String REX_FULL_REQUEST_CARD_COLLNO_SET = String.format(
            "^(%s\\s*:\\s*)?(%s\\s)?\\s*%s\\s+(\\<%s\\>)\\s*(\\(|\\[|\\{)?%s(\\s+|\\)|\\]|\\}|\\|)\\s*%s$",
            REX_DECKSEC_XMAGE, REX_CARD_COUNT, REX_CARD_NAME, REX_COLL_NUMBER, REX_SET_CODE, REX_FOIL_MTGGOLDFISH);
    public static final Pattern CARD_COLLNO_SET_PATTERN = Pattern.compile(REX_FULL_REQUEST_CARD_COLLNO_SET);
    // 6. XMage format (Amount?, [Set:Collector Number] Card Name)
    public static final String REX_FULL_REQUEST_XMAGE = String.format(
            "^(%s\\s*:\\s*)?(%s\\s)?\\s*(\\[)?%s:%s(\\])\\s+%s\\s*%s$",
            REX_DECKSEC_XMAGE, REX_CARD_COUNT, REX_SET_CODE, REX_COLL_NUMBER, REX_CARD_NAME, REX_FOIL_MTGGOLDFISH);
    public static final Pattern SET_COLLNO_CARD_XMAGE_PATTERN = Pattern.compile(REX_FULL_REQUEST_XMAGE);
    // 7. Card-Only Request (Amount?)
    public static final String REX_CARDONLY = String.format(
            "(%s\\s*:\\s*)?(%s\\s)?\\s*%s\\s*%s", REX_DECKSEC_XMAGE, REX_CARD_COUNT, REX_CARD_NAME, REX_FOIL_MTGGOLDFISH);
    public static final Pattern CARD_ONLY_PATTERN = Pattern.compile(REX_CARDONLY);

    // CoreTypes (to recognise Tokens of type CardType
    private static final CharSequence[] CARD_TYPES = allCardTypes();
    private static final CharSequence[] DECK_SECTION_NAMES = {
            "side", "sideboard", "sb",
            "main", "card", "mainboard",
            "avatar", "commander", "schemes",
            "conspiracy", "planes", "deck", "dungeon",
            "attractions", "contraptions"};

    private static CharSequence[] allCardTypes(){
        List<String> cardTypesList = new ArrayList<>();
        // CoreTypesNames
        List<CardType.CoreType> coreTypes = Lists.newArrayList(CardType.CoreType.values());
        for (CardType.CoreType coreType : coreTypes)
            cardTypesList.add(coreType.name().toLowerCase());
        // Manual Additions:
        // NOTE: "sorceries" is also included as it can be found in exported deck, even if it's incorrect.
        // Example: https://deckstats.net/decks/70852/556925-artifacts/en - see Issue 1010
        cardTypesList.add("sorceries");  // Sorcery is the only name with different plural form
        cardTypesList.add("aura");  // in case.
        cardTypesList.add("mana");  // "Mana" (see Issue 1010)
        cardTypesList.add("spell");
        cardTypesList.add("other spell");
        cardTypesList.add("planeswalker");
        return cardTypesList.toArray(new CharSequence[0]);
    }

    // These parameters are controlled only via setter methods
    private Date releaseDateConstraint = null;
    private List<String> allowedSetCodes = null;
    private List<String> gameFormatBannedCards = null;
    private List<String> gameFormatRestrictedCards = null;
    private List<DeckSection> allowedDeckSections = null;
    private boolean includeBannedAndRestricted = false;
    private DeckFormat deckFormat = null;
    private CardDb.CardArtPreference artPreference = StaticData.instance().getCardArtPreference();  // init as default

    public List<Token> parseCardList(String[] cardList) {
        List<Token> tokens = new ArrayList<>();
        DeckSection referenceDeckSectionInParsing = null;  // default
        
        for (String line : cardList) {
            Token token = this.recognizeLine(line, referenceDeckSectionInParsing);
            if (token == null)
                continue;

            TokenType tokenType = token.getType();
            if (!token.isTokenForDeck() && (tokenType != TokenType.DECK_SECTION_NAME) ||
                    (tokenType == TokenType.LIMITED_CARD && !this.includeBannedAndRestricted)) {
                // Just bluntly add the token to the list and proceed.
                tokens.add(token);
                continue;
            }

            if (token.getType() == TokenType.DECK_NAME) {
                tokens.add(0, token);  // always add deck name top of the decklist
                continue;
            }

            if (token.getType() == TokenType.DECK_SECTION_NAME) {
                referenceDeckSectionInParsing = DeckSection.valueOf(token.getText());
                tokens.add(token);
                continue;
            }

            // OK so now the token is either a Legal card or a limited card that has been marked for inclusion
            DeckSection tokenSection = token.getTokenSection();
            PaperCard tokenCard = token.getCard();

            if (isAllowed(tokenSection)) {
                if (tokenSection != referenceDeckSectionInParsing) {
                    Token sectionToken = Token.DeckSection(tokenSection.name(), this.allowedDeckSections);
                    // just check that last token is stack is a card placeholder.
                    // In that case, add the new section token before the placeholder
                    if (!tokens.isEmpty() && tokens.get(tokens.size() - 1).isCardPlaceholder())
                        tokens.add(tokens.size() - 1, sectionToken);
                    else
                        tokens.add(sectionToken);
                    referenceDeckSectionInParsing = tokenSection;
                }
                tokens.add(token);
                continue;
            }
            // So Section and Token have now been already validated in recogniseLine
            // Therefore, if the Token Section is not allowed in current Editor/Game Format,
            // the card would not be supported either.
            Token unsupportedCard = Token.UnsupportedCard(tokenCard.getName(), tokenCard.getEdition(),
                    token.getQuantity());
            tokens.add(unsupportedCard);
        }
        return tokens;
    }

    private boolean isAllowed(DeckSection tokenSection) {
        return this.allowedDeckSections == null || this.allowedDeckSections.contains(tokenSection);
    }

    public Token recognizeLine(final String rawLine, DeckSection referenceSection) {
        if (rawLine == null)
            return null;
        if (StringUtils.isBlank(rawLine.trim()))
            return null;

        final char smartQuote = (char) 8217;
        String refLine = rawLine.trim().replace(smartQuote, '\'');
        // Remove any link (e.g. Markdown Export format from TappedOut)
        refLine = purgeAllLinks(refLine);

        String line;
        if (refLine.startsWith(LINE_COMMENT_DELIMITER_OR_MD_HEADER))
            line = refLine.replaceAll(LINE_COMMENT_DELIMITER_OR_MD_HEADER, "");
        else
            line = refLine.trim();  // Remove any trailing formatting

        // Some websites export split card names with a single slash. Replace with double slash.
        // Final fantasy cards like Summon: Choco/Mog should be ommited to be recognized. TODO: fix maybe for future cards
        if (!line.contains("Summon:"))
            line = SEARCH_SINGLE_SLASH.matcher(line).replaceFirst(" // ");
        if (line.startsWith(ASTERISK))  // Markdown lists (tappedout md export)
            line = line.substring(2);

        // == Patches to Corner Cases
        // FIX Commander in Deckstats export
        if (line.endsWith("#!Commander")) {
            line = line.replaceAll("#!Commander", "");
            line = String.format("CM:%s", line.trim());
        }
        // Conspiracy section in .dec files - force to make it recognise as a placeholder
        else if (line.trim().equals("[Conspiracy]"))
            line = String.format("/ %s", line);

        Token result = recogniseCardToken(line, referenceSection);
        if (result == null)
            result = recogniseNonCardToken(line);
        return result != null ? result : refLine.startsWith(DOUBLE_SLASH) ||
                refLine.startsWith(LINE_COMMENT_DELIMITER_OR_MD_HEADER) ?
                new Token(TokenType.COMMENT, 0, refLine) : new Token(TokenType.UNKNOWN_TEXT, 0, refLine);
    }

    public static String purgeAllLinks(String line){
        String urlPattern = "(?<protocol>((https|ftp|file|http):))(?<sep>((//|\\\\)+))(?<url>([\\w\\d:#@%/;$~_?+-=\\\\.&]*))";
        Pattern p = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(line);

        while (m.find()) {
            line = line.replaceAll(m.group(), "").trim();
        }
        if (line.endsWith("()"))
            return line.substring(0, line.length()-2);
        return line;
    }

    public Token recogniseCardToken(final String text, final DeckSection currentDeckSection) {
        String line = text.trim();
        Token unknownCardToken = null;
        StaticData data = StaticData.instance();
        List<Matcher> cardMatchers = getRegExMatchers(line);
        for (Matcher matcher : cardMatchers) {
            String cardName = getRexGroup(matcher, REGRP_CARD);
            if (cardName == null)
                continue;
            cardName = cardName.trim();
            //Avoid hit the DB - check whether cardName is contained in the DB
            if (!data.isMTGCard(cardName)){
                // check the case for double-sided cards
                cardName = checkDoubleSidedCard(cardName);
            }
            String ccount = getRexGroup(matcher, REGRP_CARDNO);
            String setCode = getRexGroup(matcher, REGRP_SET);
            String collNo = getRexGroup(matcher, REGRP_COLLNR);
            String foilGr = getRexGroup(matcher, REGRP_FOIL_GFISH);
            String deckSecFromCardLine = getRexGroup(matcher, REGRP_DECK_SEC_XMAGE_STYLE);
            boolean isFoil = foilGr != null;
            int cardCount = ccount != null ? Integer.parseInt(ccount) : 1;

            if (cardName == null){
                if (ccount != null)
                    // setting cardCount to zero as the text is the whole line
                    unknownCardToken = Token.UnknownCard(text, null, 0);
                continue;
            }

            // if any, it will be tried to convert specific collector number to art index (useful for lands).
            String collectorNumber = collNo != null ? collNo : IPaperCard.NO_COLLECTOR_NUMBER;
            int artIndex;
            try {
                artIndex = Integer.parseInt(collectorNumber);
            } catch (NumberFormatException ex) {
                artIndex = IPaperCard.NO_ART_INDEX;
            }
            if (setCode != null) {
                // Ok Now we're sure the cardName is correct. Now check for setCode
                CardEdition edition = StaticData.instance().getEditions().get(setCode);
                if (edition == null) {
                    // set the case for unknown card (in case) and continue to the next for any better matching
                    unknownCardToken = Token.UnknownCard(cardName, setCode, cardCount);
                    continue;
                }

                // we now name is ok, set is ok - we just need to be sure about collector number (if any)
                // and if that card can be actually found in the requested set.
                // IOW: we should account for wrong request, e.g. Counterspell|FEM - just doesn't exist!
                PaperCard pc = data.getCardFromSet(cardName, edition, collectorNumber, artIndex, isFoil);
                if (pc != null)
                    // ok so the card has been found - let's see if there's any restriction on the set
                    return checkAndSetCardToken(pc, edition, cardCount, deckSecFromCardLine,
                                                currentDeckSection, true);
                // UNKNOWN card as in the Counterspell|FEM case
                unknownCardToken = Token.UnknownCard(cardName, setCode, cardCount);
                continue;
            }
            // ok so we can simply ignore everything but card name - as set code does not exist
            // At this stage, we know the card name exists in the DB so a Card MUST be found
            // unless it is illegal for current format or invalid with selected date.
            PaperCard pc = null;
            if (hasGameFormatConstraints()) {
                pc = data.getCardFromSupportedEditions(cardName, isFoil, this.artPreference,
                                                        this.allowedSetCodes,
                                                        this.releaseDateConstraint);
            }
            if (pc == null)
                pc = data.getCardFromSupportedEditions(cardName, isFoil, this.artPreference, null,
                                                        this.releaseDateConstraint);

            if (pc != null) {
                CardEdition edition = StaticData.instance().getCardEdition(pc.getEdition());
                return checkAndSetCardToken(pc, edition, cardCount, deckSecFromCardLine,
                                            currentDeckSection, false);
            }
        }
        return unknownCardToken;  // either null or unknown card
    }

    private String checkDoubleSidedCard(final String cardName){
        if (!cardName.contains("//"))
            return null;
        String cardRequest = cardName.trim();
        String[] sides = cardRequest.split("//");
        if (sides.length != 2)
            return null;
        String leftSide = sides[0].trim();
        String rightSide = sides[1].trim();
        StaticData data = StaticData.instance();
        if (data.isMTGCard(leftSide))
            return leftSide;
        if (data.isMTGCard(rightSide))
            return rightSide;
        return null;
    }

    private Token checkAndSetCardToken(final PaperCard pc, final CardEdition edition, final int cardCount,
                                       final String deckSecFromCardLine, final DeckSection referenceSection,
                                       final boolean cardRequestHasSetCode) {
        // Note: Always Check Allowed Set First to avoid accidentally importing invalid cards
        // e.g. Banned Cards from not-allowed sets!
        if (IsIllegalInFormat(edition.getCode()))
            // Mark as illegal card
            return Token.NotAllowedCard(pc, cardCount, cardRequestHasSetCode);

        if (isNotCompliantWithReleaseDateRestrictions(edition))
            return Token.CardInInvalidSet(pc, cardCount, cardRequestHasSetCode);

        DeckSection tokenSection = getTokenSection(deckSecFromCardLine, referenceSection, pc);
        if (isBannedInFormat(pc))
            return Token.LimitedCard(pc, cardCount, tokenSection, LimitedCardType.BANNED, cardRequestHasSetCode);

        if (isRestrictedInFormat(pc, cardCount))
            return Token.LimitedCard(pc, cardCount, tokenSection, LimitedCardType.RESTRICTED, cardRequestHasSetCode);

        return Token.LegalCard(pc, cardCount, tokenSection, cardRequestHasSetCode);
    }

    // This would save tons of time in parsing Input + would also allow to return UnsupportedCardTokens beforehand
    private DeckSection getTokenSection(String deckSec, DeckSection currentDeckSection, PaperCard card){
        if (deckSec != null) {
            DeckSection cardSection = switch (deckSec.toUpperCase().trim()) {
                case "MB" -> DeckSection.Main;
                case "SB" -> DeckSection.Sideboard;
                case "CM" -> DeckSection.Commander;
                default -> DeckSection.matchingSection(card);
            };
            if (cardSection.validate(card))
                return cardSection;
        }
        if (currentDeckSection != null){
            if (currentDeckSection.validate(card))
                return currentDeckSection;
            return DeckSection.matchingSection(card);
        }
        // When there is no reference section yet, there maybe cases in which the matched section
        // is not supported, but other possibilities exist (e.g. Commander card in Constructed
        // could potentially go in Main)
        DeckSection matchedSection = DeckSection.matchingSection(card);
        // If it's a commander candidate, put it there.
        if (matchedSection == DeckSection.Main && this.isAllowed(DeckSection.Commander) && DeckSection.Commander.validate(card))
            return DeckSection.Commander;
        if (this.isAllowed(matchedSection))
            return matchedSection;
        // if matched section is not allowed, try to match the card to main.
        // if that won't work, return matched section as this will potentially be an unsupported card!
        return DeckSection.Main.validate(card) ? DeckSection.Main : matchedSection;


    }

    private boolean hasGameFormatConstraints() {
        return (this.allowedSetCodes != null && !this.allowedSetCodes.isEmpty()) ||
                (this.gameFormatBannedCards != null && !this.gameFormatBannedCards.isEmpty()) ||
                (this.gameFormatRestrictedCards != null && !this.gameFormatRestrictedCards.isEmpty());
    }

    private String getRexGroup(Matcher matcher, String groupName){
        String rexGroup;
        try{
            rexGroup = matcher.group(groupName);
        } catch (IllegalArgumentException ex) {
            rexGroup = null;
        }
        return rexGroup;
    }

    private boolean isBannedInFormat(PaperCard pc) {
        return (this.gameFormatBannedCards != null && this.gameFormatBannedCards.contains(pc.getName())) ||
                (this.deckFormat != null && !this.deckFormat.isLegalCard(pc));
    }

    private boolean isRestrictedInFormat(PaperCard pc, int cardCount) {
        return (this.gameFormatRestrictedCards != null &&
                (this.gameFormatRestrictedCards.contains(pc.getName()) && cardCount > 1));
    }

    private boolean IsIllegalInFormat(String setCode) {
        return this.allowedSetCodes != null && !this.allowedSetCodes.contains(setCode);
    }

    private boolean isNotCompliantWithReleaseDateRestrictions(CardEdition edition){
        return this.releaseDateConstraint != null && edition.getDate().compareTo(this.releaseDateConstraint) >= 0;
    }

    private List<Matcher> getRegExMatchers(String line) {
        List<Matcher> matchers = new ArrayList<>();
        Pattern[] patternsWithCollNumber = new Pattern[] {
                CARD_SET_COLLNO_PATTERN,
                SET_CARD_COLLNO_PATTERN,
                CARD_COLLNO_SET_PATTERN,
                SET_COLLNO_CARD_XMAGE_PATTERN
        };
        for (Pattern pattern : patternsWithCollNumber) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches() && getRexGroup(matcher, REGRP_SET) != null &&
                    getRexGroup(matcher, REGRP_COLLNR) != null)
                matchers.add(matcher);
        }
        Pattern[] OtherPatterns = new Pattern[] {  // Order counts
                CARD_SET_PATTERN,
                SET_CARD_PATTERN,
                CARD_ONLY_PATTERN
        };
        for (Pattern pattern : OtherPatterns) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches())
                matchers.add(matcher);
        }
        return matchers;
    }

    public Token recogniseNonCardToken(final String text) {
        if (isDeckSectionName(text)) {
            String tokenText = nonCardTokenMatch(text);
            return Token.DeckSection(tokenText, this.allowedDeckSections);
        }
        if (isCardCMC(text)){
            String tokenText = getCardCMCMatch(text);
            return new Token(TokenType.CARD_CMC, tokenText);
        }
        if (isCardRarity(text)){
            String tokenText = cardRarityTokenMatch(text);
            if (tokenText != null && !tokenText.trim().isEmpty())
                return new Token(TokenType.CARD_RARITY, tokenText);
            return null;
        }
        if (isCardType(text)){
            String tokenText = nonCardTokenMatch(text);
            return new Token(TokenType.CARD_TYPE, tokenText);
        }
        if(isManaToken(text)){
            String tokenText = getManaTokenMatch(text);
            return new Token(TokenType.MANA_COLOUR, tokenText);
        }
        if (isDeckName(text)) {
            String deckName = deckNameMatch(text);
            return new Token(TokenType.DECK_NAME, deckName.trim());
        }
        return null;
    }

    /* -----------------------------------------------------------------------------
    Note: Card types, CMC, and Rarity Tokens are **only** used for style formatting
    in the Import Editor. This won't affect the import process in any way.
    The use of these tokens has been borrowed by Deckstats.net format export.
    ----------------------------------------------------------------------------- */
    public static boolean isCardType(final String lineAsIs) {
        String nonCardToken = nonCardTokenMatch(lineAsIs);
        if (nonCardToken == null)
            return false;
        return StringUtils.startsWithAny(nonCardToken.toLowerCase(), CARD_TYPES);
    }

    public static boolean isCardRarity(final String lineAsIs){
        return cardRarityTokenMatch(lineAsIs) != null;
    }

    public static boolean isCardCMC(final String lineAsIs) {
        return cardCMCTokenMatch(lineAsIs) != null;
    }

    public static boolean isManaToken(final String lineAsIs) {
        return manaTokenMatch(lineAsIs) != null;
    }

    public static boolean isDeckSectionName(final String lineAsIs) {
        String nonCardToken = nonCardTokenMatch(lineAsIs);
        if (nonCardToken == null)
            return false;
        return StringUtils.equalsAnyIgnoreCase(nonCardToken, DECK_SECTION_NAMES);
    }

    private static String nonCardTokenMatch(final String lineAsIs){
        if (lineAsIs == null)
            return null;
        String line = lineAsIs.trim();
        Matcher noncardMatcher = NONCARD_PATTERN.matcher(line);
        if (!noncardMatcher.matches())
            return null;
        return noncardMatcher.group(REGRP_TOKEN);
    }

    private static String cardRarityTokenMatch(final String lineAsIs){
        if (lineAsIs == null)
            return null;
        String line = lineAsIs.trim();
        Matcher cardRarityMatcher = CARD_RARITY_PATTERN.matcher(line);
        if (!cardRarityMatcher.matches())
            return null;
        return cardRarityMatcher.group(REGRP_TOKEN);
    }

    private static String cardCMCTokenMatch(final String lineAsIs){
        if (lineAsIs == null)
            return null;
        String line = lineAsIs.trim();
        Matcher cardCMCmatcher = CMC_PATTERN.matcher(line);
        if (!cardCMCmatcher.matches())
            return null;
        return cardCMCmatcher.group(REGRP_TOKEN);
    }

    private String getCardCMCMatch(String lineAsIs) {
        String tokenMatch = cardCMCTokenMatch(lineAsIs);
        tokenMatch = tokenMatch.toUpperCase();
        if (tokenMatch.contains("CC"))
            tokenMatch = tokenMatch.replaceAll("CC", "").trim();
        else
            tokenMatch = tokenMatch.replaceAll("CMC", "").trim();
        return String.format("CMC: %s", tokenMatch);
    }

    private static Pair<String, String> manaTokenMatch(final String lineAsIs){
        if (lineAsIs == null)
            return null;
        String line = lineAsIs.trim();
        Matcher manaMatcher = MANA_PATTERN.matcher(line);
        if (!manaMatcher.matches())
            return null;
        String firstMana = manaMatcher.group(REGRP_COLR1);
        String secondMana = manaMatcher.group(REGRP_COLR2);
        firstMana = matchAnyManaSymbolIn(firstMana);
        secondMana = matchAnyManaSymbolIn(secondMana);
        return Pair.of(firstMana, secondMana);
    }

    private static String matchAnyManaSymbolIn(String manaToken){
        if (manaToken == null)
            return null;
        Matcher matchManaSymbol = MANA_SYMBOL_PATTERN.matcher(manaToken);
        if (matchManaSymbol.matches())
            return matchManaSymbol.group(REGRP_MANA);
        return manaToken;
    }

    private static String getManaTokenMatch(final String lineAsIs){
        Pair<String, String> matchedMana = manaTokenMatch(lineAsIs);
        String color1name = matchedMana.getLeft();
        String color2name = matchedMana.getRight();

        MagicColor.Color magicColor;
        MagicColor.Color magicColor2;
        if (color1name.length() == 2) { // the only case possible for this to happen is two colour codes
            magicColor  = getMagicColor(color1name.substring(0, 1));
            magicColor2 = getMagicColor(color1name.substring(1));
            return getMagicColourLabel(magicColor, magicColor2);
        }
        magicColor = getMagicColor(color1name);
        if (color2name == null)
            return getMagicColourLabel(magicColor);
        magicColor2 = getMagicColor(color2name);
        if (magicColor2 == magicColor)
            return getMagicColourLabel(magicColor);
        return getMagicColourLabel(magicColor, magicColor2);
    }

    private static String getMagicColourLabel(MagicColor.Color magicColor) {
        if (magicColor == null) // Multicolour
            return String.format("%s {W}{U}{B}{R}{G}", getLocalisedMagicColorName("Multicolour"));
        return String.format("%s %s", magicColor.getLocalizedName(), magicColor.getSymbol());
    }

    private static final HashMap<Integer, String> manaSymbolsMap = new HashMap<Integer, String>() {{
        put(MagicColor.WHITE | MagicColor.BLUE, "WU");
        put(MagicColor.BLUE | MagicColor.BLACK, "UB");
        put(MagicColor.BLACK | MagicColor.RED, "BR");
        put(MagicColor.RED | MagicColor.GREEN, "RG");
        put(MagicColor.GREEN | MagicColor.WHITE, "GW");
        put(MagicColor.WHITE | MagicColor.BLACK, "WB");
        put(MagicColor.BLUE | MagicColor.RED, "UR");
        put(MagicColor.BLACK | MagicColor.GREEN, "BG");
        put(MagicColor.RED | MagicColor.WHITE, "RW");
        put(MagicColor.GREEN | MagicColor.BLUE, "GU");
    }};
    private static String getMagicColourLabel(MagicColor.Color magicColor1, MagicColor.Color magicColor2){
        if (magicColor2 == null || magicColor2 == MagicColor.Color.COLORLESS
                || magicColor1 == MagicColor.Color.COLORLESS)
            return String.format("%s // %s", getMagicColourLabel(magicColor1), getMagicColourLabel(magicColor2));
        String localisedName1 = magicColor1.getLocalizedName();
        String localisedName2 = magicColor2.getLocalizedName();
        String comboManaSymbol = manaSymbolsMap.get(magicColor1.getColorMask() | magicColor2.getColorMask());
        return String.format("%s/%s {%s}", localisedName1, localisedName2, comboManaSymbol);
    }

    private static MagicColor.Color getMagicColor(String colorName){
        if (colorName.toLowerCase().startsWith("multi") || colorName.equalsIgnoreCase("m"))
            return null;  // will be handled separately
        return MagicColor.Color.fromByte(MagicColor.fromName(colorName.toLowerCase()));
    }

    public static String getLocalisedMagicColorName(String colorName){
        Localizer localizer = Localizer.getInstance();
        return switch (colorName.toLowerCase()) {
            case MagicColor.Constant.WHITE -> localizer.getMessage("lblWhite");
            case MagicColor.Constant.BLUE -> localizer.getMessage("lblBlue");
            case MagicColor.Constant.BLACK -> localizer.getMessage("lblBlack");
            case MagicColor.Constant.RED -> localizer.getMessage("lblRed");
            case MagicColor.Constant.GREEN -> localizer.getMessage("lblGreen");
            case MagicColor.Constant.COLORLESS -> localizer.getMessage("lblColorless");
            case "multicolour", "multicolor" -> localizer.getMessage("lblMulticolor");
            default -> "";
        };
    }

    /**
     * Get the magic color by the localised/translated name.
     * @param localisedName String of localised color name.
     * @return The string of the magic color.
     */
    public static String getColorNameByLocalisedName(String localisedName) {
        Localizer localizer = Localizer.getInstance();

        if(localisedName.equals(localizer.getMessage("lblWhite"))) return MagicColor.Constant.WHITE;
        if(localisedName.equals(localizer.getMessage("lblBlue"))) return MagicColor.Constant.BLUE;
        if(localisedName.equals(localizer.getMessage("lblBlack"))) return MagicColor.Constant.BLACK;
        if(localisedName.equals(localizer.getMessage("lblRed"))) return MagicColor.Constant.RED;
        if(localisedName.equals(localizer.getMessage("lblGreen"))) return MagicColor.Constant.GREEN;

        return "";
    }
    public static boolean isDeckName(final String lineAsIs) {
        if (lineAsIs == null)
            return false;
        final String line = lineAsIs.trim();
        final Matcher deckNameMatcher = DECK_NAME_PATTERN.matcher(line);
        return deckNameMatcher.matches();
    }

    public static String deckNameMatch(final String text) {
        if (text == null)
            return "";
        String line = text.trim();
        final Matcher deckNamePattern = DECK_NAME_PATTERN.matcher(line);
        if (deckNamePattern.matches())
            return deckNamePattern.group(REGRP_DECKNAME);  // Deck name is at match 7
        return "";
    }

    public void setDateConstraint(int year, int month) {
        Calendar ca = Calendar.getInstance();
        ca.set(year, month, 1);
        releaseDateConstraint = ca.getTime();
    }

    public void setGameFormatConstraint(List<String> allowedSetCodes,
                                        List<String> bannedCards, List<String> restrictedCards){
        if (allowedSetCodes != null && !allowedSetCodes.isEmpty())
            this.allowedSetCodes = allowedSetCodes;
        else
            this.allowedSetCodes = null;

        if (bannedCards != null && !bannedCards.isEmpty())
            this.gameFormatBannedCards = bannedCards;
        else
            this.gameFormatBannedCards = null;

        if (restrictedCards != null && !restrictedCards.isEmpty())
            this.gameFormatRestrictedCards = restrictedCards;
        else
            this.gameFormatRestrictedCards = null;

    }

    public void setDeckFormatConstraint(DeckFormat deckFormat0){
        this.deckFormat = deckFormat0;
    }

    public void setArtPreference(CardDb.CardArtPreference artPref){ this.artPreference = artPref; }

    public void setAllowedDeckSections(List<DeckSection> deckSections){ this.allowedDeckSections = deckSections; }

    public void forceImportBannedAndRestrictedCards() { this.includeBannedAndRestricted = true; }
}
