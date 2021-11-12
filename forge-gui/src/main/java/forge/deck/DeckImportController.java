package forge.deck;

import java.text.DateFormatSymbols;
import java.util.*;

import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.deck.DeckRecognizer.TokenType;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.deck.DeckRecognizer.Token;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import org.apache.commons.lang3.StringUtils;

import forge.gui.interfaces.ICheckBox;
import forge.gui.interfaces.IComboBox;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.util.Localizer;
import org.apache.commons.lang3.tuple.Pair;

public class DeckImportController {
    private boolean createNewDeck;
    // Date filter
    private final ICheckBox dateTimeCheck;
    private final IComboBox<String> monthDropdown;
    private final IComboBox<Integer> yearDropdown;
    // CardArt Preference Filter
    private CardDb.CardArtPreference artPreference;
    private boolean smartCardArt;
    // Block Preference Filter
    private boolean inlcludeBnRInDeck = false;

    private final List<Token> tokens = new ArrayList<>();
    private final Map<PaperCard, Token> cardsInTokens = new HashMap<>();
    private final boolean currentDeckNotEmpty;
    private Deck currentDeckInEditor = null;
    private DeckFormat currentDeckFormat;
    private GameFormat currentGameFormat;
    private final List<DeckSection> allowedSections = new ArrayList<>();

    public DeckImportController(ICheckBox dateTimeCheck0,
                                IComboBox<String> monthDropdown0, IComboBox<Integer> yearDropdown0,
                                boolean currentDeckNotEmpty) {

        this.dateTimeCheck = dateTimeCheck0;
        this.monthDropdown = monthDropdown0;
        this.yearDropdown = yearDropdown0;
        /* This keeps track whether the current deck in editor is **not empty**.
           If that is the case, and the "Replace" option won't be checked, this will
           allow to ask for confirmation whether the *Merge* action is what
           really intended by the user!
         */
        this.currentDeckNotEmpty = currentDeckNotEmpty;
        // this option will control the "new deck" action controlled by UI widget
        createNewDeck = false;

        // Init default parameters
        this.artPreference = StaticData.instance().getCardArtPreference();  // default
        this.smartCardArt = StaticData.instance().isEnabledCardArtSmartSelection();
        this.currentDeckFormat = null;
        this.currentGameFormat = null;
        fillDateDropdowns();
    }

    public void setGameFormat(GameType gameType){
        if (gameType == null){
            this.currentGameFormat = null;
            this.currentDeckFormat = null;
        } else {
            // get the game format with the same name of current game type (if any)
            this.currentDeckFormat = gameType.getDeckFormat();
            this.currentGameFormat = FModel.getFormats().get(gameType.name());
        }
    }

    public void setCurrentDeckInEditor(Deck deckInEditor){
        this.currentDeckInEditor = deckInEditor;
    }

    public void setAllowedSections(List<DeckSection> allSections){
        this.allowedSections.addAll(allSections);
    }

    public boolean hasNoDefaultGameFormat(){
        return this.currentGameFormat == null;
    }

    public String getCurrentGameFormatName(){
        if (this.currentGameFormat == null)
            return "";
        return this.currentGameFormat.getName();
    }

    public void setCardArtPreference(boolean isLatest, boolean coreFilterEnabled){
        this.artPreference = StaticData.instance().getCardArtPreference(isLatest, coreFilterEnabled);
    }

    public void setSmartCardArtOptimisation(boolean enableSmartArt){
        this.smartCardArt = enableSmartArt;
    }

    public boolean isSmartCardArtEnabled(){
        return this.smartCardArt;
    }

    public void setCreateNewDeck(boolean createNewDeck){
        this.createNewDeck = createNewDeck;
    }

    public boolean getCreateNewDeck() { return this.createNewDeck; }

    private void fillDateDropdowns() {
        DateFormatSymbols dfs = new DateFormatSymbols();
        monthDropdown.removeAllItems();
        String[] months = dfs.getMonths();
        for (String monthName : months) {
            if (!StringUtils.isBlank(monthName)) {
                monthDropdown.addItem(monthName);
            }
        }
        int yearNow = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = yearNow; i >= 1993; i--) {
            yearDropdown.addItem(i);
        }
    }

    public void fillFormatDropdown(IComboBox<GameFormat> formatsDropdown){
        if (formatsDropdown == null)
            return;
        formatsDropdown.removeAllItems();
        // If the current Game format is already set, no format selection is allowed
        if (this.currentGameFormat == null) {
            final GameFormat SEPARATOR = GameFormat.NoFormat;
            final Iterable<GameFormat> sanctionedFormats = FModel.getFormats().getSanctionedList();
            for (final GameFormat f : sanctionedFormats)
                formatsDropdown.addItem(f);

            if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.LOAD_HISTORIC_FORMATS)) {
                // Add Block Formats
                formatsDropdown.addItem(SEPARATOR);
                final Iterable<GameFormat> blockFormats = FModel.getFormats().getBlockList();
                for (final GameFormat f : blockFormats)
                    formatsDropdown.addItem(f);
            }
        }
    }

    public void setCurrentGameFormat(GameFormat gameFormat){
        this.currentGameFormat = gameFormat;
    }

    public void importBannedAndRestrictedCards(boolean includeBannedAndRestricted){
        this.inlcludeBnRInDeck = includeBannedAndRestricted;
    }

    public boolean importBannedAndRestrictedCards(){ return this.inlcludeBnRInDeck; }

    public List<Token> parseInput(String input) {
        tokens.clear();
        cardsInTokens.clear();
        DeckRecognizer recognizer = new DeckRecognizer();
        // Set Art Preference first thing
        recognizer.setArtPreference(this.artPreference);
        // Edition Release Date Constraint
        if (dateTimeCheck.isSelected())
            recognizer.setDateConstraint(yearDropdown.getSelectedItem(), monthDropdown.getSelectedIndex());
        // Game Format Constraint
        if (this.currentGameFormat != null){
            recognizer.setGameFormatConstraint(this.currentGameFormat.getAllowedSetCodes(),
                                               this.currentGameFormat.getBannedCardNames(),
                                               this.currentGameFormat.getRestrictedCards());
        }
        // Deck Format Constraint
        if (this.currentDeckFormat != null)
            recognizer.setDeckFormatConstraint(this.currentDeckFormat);
        // (Current Editor) Deck Sections Constraint
        if (!this.allowedSections.isEmpty())
            recognizer.setAllowedDeckSections(this.allowedSections);
        // Banned and Restricted Card Policy
        if (this.inlcludeBnRInDeck)
            recognizer.forceImportBannedAndRestrictedCards();

        String[] lines = input.split("\n");
        List<Token> parsedTokens = recognizer.parseCardList(lines);
        if (parsedTokens != null)
            tokens.addAll(parsedTokens);

        if (this.currentGameFormatAllowsCommander()) {
            List<Pair<Integer, Token>> commanderTokens = getTokensInSection(DeckSection.Commander);
            if (commanderTokens.isEmpty())  // Check commanders in Sideboard only if the commander section is empty
                checkAndFixCommanderIn(DeckSection.Sideboard);
            checkAndFixCommanderIn(DeckSection.Commander);
        }

        collectAllCardsInTokens();
        return tokens;
    }

    private void collectAllCardsInTokens(){
        cardsInTokens.clear();
        for (Token token : tokens){
            if (!token.isCardToken())
                continue;
            PaperCard tokenCard = token.getCard();
            cardsInTokens.put(tokenCard, token);
        }
    }

    private void checkAndFixCommanderIn(DeckSection targetDeckSection){
        // first get all tokens in sideboard, along with corresponding index
        List<Pair<Integer, Token>> sectionTokens = getTokensInSection(targetDeckSection);
        List<Pair<Integer, Token>> candidateCommanderTokens = getAllCommanderTokens(sectionTokens);

        if (candidateCommanderTokens.isEmpty())
            return;
        int commandersCandidateCount = 0;
        for (Pair<Integer, Token> ccTokenPair : candidateCommanderTokens)
            commandersCandidateCount += ccTokenPair.getRight().getQuantity();

        if (commandersCandidateCount > 1){
            String msg = getListOfCandidateCommandersIn(targetDeckSection, candidateCommanderTokens,
                    commandersCandidateCount);
            Token warningMsg = Token.WarningMessage(msg);

            // Try to add the warning message right after the deck section placeholder
            int targetSecTokenIndex = candidateCommanderTokens.get(0).getLeft() - 1;
            Token tokenInList = tokens.get(targetSecTokenIndex);
            while (!tokenInList.isDeckSection()){
                targetSecTokenIndex -= 1;
                if (targetSecTokenIndex < 0)
                    break;
                tokenInList = tokens.get(targetSecTokenIndex);
            }
            if (targetSecTokenIndex >= 0 && targetSecTokenIndex + 1 < tokens.size())
                tokens.add(targetSecTokenIndex+1, warningMsg);
            else
                tokens.add(warningMsg);
            return;
        }

        if (commandersCandidateCount == 1 && targetDeckSection == DeckSection.Commander)
            return;  // all clear, nothing to do here

        // Last case is that there's only one single candidate
        Pair<Integer, Token> commanderTokenPair = candidateCommanderTokens.get(0);
        int tokenIndex = commanderTokenPair.getLeft();
        Token commanderToken = commanderTokenPair.getRight();
        String msg = Localizer.getInstance().getMessage("lblWarnCardInInvalidSection",
                commanderToken.getText(), targetDeckSection.name(), DeckSection.Commander.name());
        Token cardInInvalidSectionToken = Token.WarningMessage(msg);
        // Reset section in token, for correct card import
        commanderToken.resetTokenSection(DeckSection.Commander);

        // Check that there is a (old) Section token in Decklist, just before the target token
        if (tokenIndex-1 >= 0 && tokens.get(tokenIndex - 1).isDeckSection() &&
                tokens.get(tokenIndex - 1).getText().equals(targetDeckSection.name())){
            // if the card to be moved is preceded by a DeckSection token
            tokens.remove(tokenIndex -1);
            tokens.add(tokenIndex-1,
                    Token.DeckSection(DeckSection.Commander.name(), this.allowedSections));
        }
        tokens.add(tokenIndex, cardInInvalidSectionToken);
    }

    private String getListOfCandidateCommandersIn(DeckSection targetSection, List<Pair<Integer, Token>> candidateCommandersInSide,
                                                  int cardsNumber) {
        StringBuilder commandersCardNames = new StringBuilder();
        for (Pair<Integer, Token> ccTokenPair : candidateCommandersInSide){
            Token ccToken = ccTokenPair.getRight();
            commandersCardNames.append(String.format("\n- %d x %s", ccToken.getQuantity(), ccToken.getText()));
        }
        String msg = Localizer.getInstance().getMessage("lblWarnTooManyCommanders", targetSection.name(),
                cardsNumber, commandersCardNames.toString());
        if (targetSection != DeckSection.Commander)
            return String.format("%s\n%s", msg, Localizer.getInstance().getMessage("lblWarnCommandersInSideExtra"));
        return msg;
    }

    private List<Pair<Integer, Token>> getAllCommanderTokens(List<Pair<Integer, Token>> sectionTokenPairs) {
        List<Pair<Integer, Token>> candidateCommandersInSide = new ArrayList<>();
        for (Pair<Integer, Token> secTokenPair : sectionTokenPairs){
            Token secToken = secTokenPair.getRight();
            PaperCard card = secToken.getCard();
            if (card != null && DeckSection.Commander.validate(card))
                candidateCommandersInSide.add(secTokenPair);
        }
        return candidateCommandersInSide;
    }

    private List<Pair<Integer, Token>> getTokensInSection(DeckSection section) {
        List<Pair<Integer, Token>> tokensInSection = new ArrayList<>();
        for (int idx = 0; idx < tokens.size(); idx++) {
            Token token = tokens.get(idx);
            DeckSection tokenSection = token.getTokenSection();
            if (tokenSection != section)
                continue;
            tokensInSection.add(Pair.of(idx, token));
        }
        return tokensInSection;
    }

    public boolean currentGameFormatAllowsCommander(){
        return this.allowedSections.contains(DeckSection.Commander);
    }

    public List<Token> optimiseCardArtInTokens(){
        /* == STEP 1. Collect info about tokens to optimise

        Organise card tokens (per section) into two groups, depending on whether they have or not
        the edition specified in original request.
        If no tokens with NO set will be found, there is no optimisation to run, so we could
        skip remaining steps.
         */
        Map<DeckSection, List<Token>> tokensPerSectionWithSet = new HashMap<>();
        Map<DeckSection, List<Token>> tokensPerSectionWithNoSet = new HashMap<>();
        for (Token token : this.tokens){
            if (!token.isCardTokenForDeck())
                continue;
            DeckSection tokenSection = token.getTokenSection();
            Map<DeckSection, List<Token>> refTokenMap;
            if (token.cardRequestHasNoCode())
                refTokenMap = tokensPerSectionWithNoSet;
            else
                refTokenMap = tokensPerSectionWithSet;

            List<Token> tokensInSection = refTokenMap.getOrDefault(tokenSection, null);
            if (tokensInSection == null) {
                tokensInSection = new ArrayList<>();
                tokensInSection.add(token);
                refTokenMap.put(tokenSection, tokensInSection);
            } else
                tokensInSection.add(token);
        }

        if (tokensPerSectionWithNoSet.isEmpty())
            return tokens; // NO Optimisation needed.

        /* == STEP 2. Set up the reference pool of cards for optimisation per each section.
            - we will start by considering whether we should include or not current deck;
            - we will then consider the tokens with set collected in previous step.

            In the end, if cards with specified set (either in tokens or in current deck, if any)
            account for less than the 50% of total card counts (excl. basic lands), the whole
            pool of cards will be considered for optimisation - similarly to what happens with
            Decks with no editions, e.g. Net decks.
        */

        Map<DeckSection, CardPool> referencePoolPerSection = new HashMap<>();

        if (this.currentDeckNotEmpty && !this.createNewDeck && this.currentDeckInEditor != null){
            // We will always consider ONLY sections for cards needing art optimisation
            for (DeckSection section : tokensPerSectionWithNoSet.keySet()){
                CardPool cardsInDeck = this.currentDeckInEditor.get(section);
                if (cardsInDeck == null || cardsInDeck.isEmpty())
                    continue;
                CardPool optCardPool = new CardPool(cardsInDeck);
                referencePoolPerSection.put(section, optCardPool);
            }
        }

        // Now check tokens with set wrt. tokens with no set
        for (DeckSection section: tokensPerSectionWithNoSet.keySet()){
            List<Token> sectionTokensNoSet = tokensPerSectionWithNoSet.get(section);
            List<Token> sectionTokenWithSet = tokensPerSectionWithSet.getOrDefault(section, null);

            CardPool sectionCardPool = referencePoolPerSection.getOrDefault(section, null);
            if (sectionCardPool == null)  // No current deck, or deck has that section empty
                sectionCardPool = new CardPool();

            int tokensWithSetCount = countTokens(sectionTokenWithSet);
            int cardsInPoolCount = sectionCardPool.countAll();
            int tokensNoSetCount = countTokens(sectionTokensNoSet);
            int totalCount = tokensNoSetCount + tokensWithSetCount + cardsInPoolCount;
            if (totalCount == 0)
                continue;
            float cardsWithSetRatio = (float)(tokensWithSetCount + cardsInPoolCount) / totalCount;

            // If all cards in section are missing or
            if (cardsWithSetRatio < 0.5) {
                for (Token t: sectionTokensNoSet)
                    sectionCardPool.add(t.getCard(), t.getQuantity());
            }

            if (sectionTokenWithSet != null){
                for (Token t: sectionTokenWithSet)
                    sectionCardPool.add(t.getCard(), t.getQuantity());
            }

            referencePoolPerSection.put(section, sectionCardPool);
        }

        /* == STEP 3. Optimise card art in tokens
        Now we do have collected the reference pool of cards. We can now proceed with
        the final optimisation step
         */
        StaticData data = StaticData.instance();
        boolean isCardArtPreferenceLatestArt = this.artPreference.latestFirst;
        boolean cardArtPreferenceHasFilter = this.artPreference.filterSets;
        List<String> allowedSetCodes = this.currentGameFormat != null ? this.currentGameFormat.getAllowedSetCodes() : null;
        for (DeckSection section: tokensPerSectionWithNoSet.keySet()){
            CardPool cardArtReferencePool = referencePoolPerSection.get(section);
            if (cardArtReferencePool == null || cardArtReferencePool.isEmpty())
                continue;  // nothing to do here.
            boolean isExpansionTheMajorityInThePool = (cardArtReferencePool.getTheMostFrequentEditionType() == CardEdition.Type.EXPANSION);
            boolean isPoolModernFramed = cardArtReferencePool.isModern();
            CardEdition pivotEdition = cardArtReferencePool.getPivotCardEdition(isCardArtPreferenceLatestArt);
            Date releaseDatePivotEdition = pivotEdition.getDate();

            List<Token> tokensToOptimise = tokensPerSectionWithNoSet.get(section);
            for (Token t: tokensToOptimise){
                PaperCard tokenCard = t.getCard();
                PaperCard alternativeCardPrint = data.getAlternativeCardPrint(tokenCard, releaseDatePivotEdition,
                                                                                isCardArtPreferenceLatestArt,
                                                                                cardArtPreferenceHasFilter,
                                                                                isExpansionTheMajorityInThePool,
                                                                                isPoolModernFramed, allowedSetCodes);
                if (alternativeCardPrint != null)
                    t.replaceTokenCard(alternativeCardPrint);
            }
        }

        // Regenerate cardsInTokens Map
        collectAllCardsInTokens();

        return tokens;
    }

    private int countTokens(List<Token> tokensInSection){
        if (tokensInSection == null || tokensInSection.isEmpty())
            return 0;
        int tokensCount = 0;
        for (Token t: tokensInSection){
            if (!t.isCardTokenForDeck())
                continue;
            PaperCard tCard = t.getCard();
            if (tCard.isVeryBasicLand())
                continue;
            tokensCount += t.getQuantity();
        }
        return tokensCount;
    }

    public PaperCard getCardFromDecklist(final PaperCard card){
        if (cardsInTokens.containsKey(card))
            return card; // found - same instance returned

        // Account for any [un]foiled version
        PaperCard cardKey;
        if (card.isFoil())
            cardKey = new PaperCard(card.getRules(), card.getEdition(), card.getRarity(), card.getArtIndex(),
                               false, card.getCollectorNumber(), card.getArtist());
        else
            cardKey = card.getFoiled();

        return cardsInTokens.containsKey(cardKey) ? cardKey : null;
    }

    public PaperCard getCardFromDecklistByName(String cardName){
        for (PaperCard cardKey : this.cardsInTokens.keySet()){
            if (!cardKey.getName().equals(cardName))
                continue;
            return cardKey;
        }
        return null;
    }

    public Token getTokenFromCardInDecklist(PaperCard cardKey){
        return this.cardsInTokens.getOrDefault(cardKey, null);
    }

    public Deck accept(){
        return this.accept("");
    }

    public Deck accept(String currentDeckName) {
        final Localizer localizer = Localizer.getInstance();
        if (tokens.isEmpty()) { return null; }

        String deckName = "";
        if (currentDeckName != null && currentDeckName.trim().length() > 0)
            deckName = String.format("\"%s\"", currentDeckName.trim());

        String tokenDeckName = getTokenDeckNameIfAny();
        if (tokenDeckName.length() > 0)
            tokenDeckName = String.format("\"%s\"", tokenDeckName);

        if (createNewDeck){
            String extraWarning = currentDeckNotEmpty ? localizer.getMessage("lblNewDeckWarning", deckName) : "";
            final String warning = localizer.getMessage("lblConfirmCreateNewDeck", tokenDeckName, extraWarning);
            if (!SOptionPane.showConfirmDialog(warning, localizer.getMessage("lblNewDeckDialogTitle"),
                    localizer.getMessage("lblYes"), localizer.getMessage("lblNo"))) {
                return null;
            }
        }
        else if (this.currentDeckNotEmpty){
            String extraWarning = (tokenDeckName.length() > 0 && !tokenDeckName.equals(deckName)) ?
                    localizer.getMessage("lblCardImportWarning", deckName, tokenDeckName) : "";
            final String warning = localizer.getMessage("lblConfirmCardImport", deckName, extraWarning);
            if (!SOptionPane.showConfirmDialog(warning,
                    localizer.getMessage("lblImportCardsDialogTitle"),
                    localizer.getMessage("lblYes"), localizer.getMessage("lblNo")))
                return null;
        }
        final Deck resultDeck = new Deck();
        for (final Token t : tokens) {
            final TokenType type = t.getType();
            // only Deck Name, legal card and limited card tokens will be analysed!
            if (!t.isTokenForDeck() ||
                    (type == TokenType.LIMITED_CARD && !this.inlcludeBnRInDeck))
                continue;  // SKIP token

            if (type == TokenType.DECK_NAME) {
                resultDeck.setName(t.getText());
                continue;
            }

            final DeckSection deckSection = t.getTokenSection();
            final PaperCard crd = t.getCard();
            /* Deck Sections have been already validated for tokens by DeckRecogniser,
             * plus any other adjustment (like accounting for Commander in Sideboard) has been
             * already taken care of in previous parseInput.
             * Therefore, we can safely proceed here by just adding the cards. */
            resultDeck.getOrCreate(deckSection).add(crd, t.getQuantity());
        }
        return resultDeck;
    }

    private String getTokenDeckNameIfAny(){
        for (final Token t : this.tokens){
            // only Deck Name, legal card and limited card tokens will be analysed!
            if (!t.isTokenForDeck())
                continue;  // SKIP token
            final TokenType tType = t.getType();
            if (tType == TokenType.DECK_NAME) {
                return t.getText();
            }
        }
        return "";  // no deck name
    }
}
