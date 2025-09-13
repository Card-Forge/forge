package forge.deck;

import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.deck.DeckRecognizer.Token;
import forge.deck.DeckRecognizer.TokenType;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.gui.interfaces.ICheckBox;
import forge.gui.interfaces.IComboBox;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.ItemPool;
import forge.util.Localizer;
import forge.util.StreamUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.text.DateFormatSymbols;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DeckImportController {
    public enum ImportBehavior {
        MERGE,
        CREATE_NEW,
        REPLACE_CURRENT
    }

    private ImportBehavior importBehavior;
    // Date filter
    private final ICheckBox dateTimeCheck;
    private final IComboBox<String> monthDropdown;
    private final IComboBox<Integer> yearDropdown;
    // CardArt Preference Filter
    private CardDb.CardArtPreference artPreference;
    private boolean smartCardArt;
    // Block Preference Filter
    private boolean includeBnRInDeck = false;

    private final List<Token> tokens = new ArrayList<>();
    private final Map<PaperCard, Token> cardsInTokens = new HashMap<>();
    private final boolean currentDeckNotEmpty;
    private Deck currentDeckInEditor = null;
    private DeckFormat currentDeckFormat;
    private GameFormat currentGameFormat;
    private GameType currentGameType;
    private final List<DeckSection> allowedSections = new ArrayList<>();
    private ItemPool<PaperCard> playerInventory;
    /**
     * If a free card is missing from a player's inventory (e.g. a basic land), it gets run through this function, which
     * can handle creation of a usable print.
     */
    private Function<PaperCard, PaperCard> freePrintSupplier;

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
        this.importBehavior = ImportBehavior.MERGE;

        // Init default parameters
        this.artPreference = StaticData.instance().getCardArtPreference();  // default
        this.smartCardArt = StaticData.instance().isEnabledCardArtSmartSelection();
        this.currentDeckFormat = null;
        this.currentGameFormat = null;
        this.currentGameType = null;
        fillDateDropdowns();
    }

    public void setGameFormat(GameType gameType){
        if (gameType == null){
            this.currentGameFormat = null;
            this.currentDeckFormat = null;
            this.currentGameType = null;
        } else {
            // get the game format with the same name of current game type (if any)
            this.currentDeckFormat = gameType.getDeckFormat();
            this.currentGameFormat = FModel.getFormats().get(gameType.name());
            this.currentGameType = gameType;
        }
    }

    public void setPlayerInventory(ItemPool<PaperCard> inventory) {
        this.playerInventory = inventory;
    }

    public void setFreePrintConverter(Function<PaperCard, PaperCard> freePrintSupplier) {
        this.freePrintSupplier = freePrintSupplier;
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

    public void setImportBehavior(ImportBehavior importBehavior) {
        this.importBehavior = importBehavior;
    }

    public ImportBehavior getImportBehavior() {
        return importBehavior;
    }

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

            // Casual Formats
            // Add Block Formats
            formatsDropdown.addItem(SEPARATOR);
            final Iterable<GameFormat> casualFormats = FModel.getFormats().getCasualList();
            for (final GameFormat f : casualFormats)
                formatsDropdown.addItem(f);

            if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.LOAD_ARCHIVED_FORMATS)) {
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
        this.includeBnRInDeck = includeBannedAndRestricted;
    }

    public boolean importBannedAndRestrictedCards(){ return this.includeBnRInDeck; }

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
        if (this.includeBnRInDeck)
            recognizer.forceImportBannedAndRestrictedCards();

        String[] lines = input.split("\n");
        List<Token> parsedTokens = recognizer.parseCardList(lines);
        if (parsedTokens != null)
            tokens.addAll(parsedTokens);

        if (this.currentGameFormatAllowsCommander()) {
            List<Pair<Integer, Token>> commanderTokens = getTokensInSection(DeckSection.Commander);
            if (commanderTokens.isEmpty()) {
                // Check commanders in Sideboard only if the commander section is empty
                if(!getTokensInSection(DeckSection.Sideboard).isEmpty())
                    checkAndFixCommanderIn(DeckSection.Sideboard);
                else
                    checkAndFixCommanderIn(DeckSection.Main);
            }
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
        return this.allowedSections.contains(DeckSection.Commander) || this.currentGameType == GameType.PlanarConquest;
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

            List<Token> tokensInSection = refTokenMap.computeIfAbsent(tokenSection, e -> new ArrayList<>());
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

        if (this.currentDeckNotEmpty && this.importBehavior == ImportBehavior.MERGE && this.currentDeckInEditor != null){
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
            if (pivotEdition == null)
                continue;
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

    public List<Token> constrainTokensToInventory() {
        if(this.playerInventory == null)
            return tokens;
        CardPool availableInventory = new CardPool(this.playerInventory);
        //Map of tokens to the things we're gonna replace them with.
        Map<Token, List<Token>> tokenReplacers = new LinkedHashMap<>();
        //If we're adding to our existing deck, ensure we aren't counting the cards already in it.
        if(this.importBehavior == ImportBehavior.MERGE && this.currentDeckInEditor != null)
            availableInventory.removeAll(this.currentDeckInEditor.getAllCardsInASinglePool(true, true));
        if(this.currentGameType == GameType.PlanarConquest && currentDeckInEditor != null)
            availableInventory.removeAllFlat(this.currentDeckInEditor.getCommanders());
        //Step 1: For each token, if it's asking for more copies of a print than we can supply, split the difference out
        //into a token that's indifferent to the edition. Reduce available inventory accordingly.
        for (Token token : this.tokens) {
            if (!token.isCardToken())
                continue;
            PaperCard card = token.getCard();
            int requestedAmount = token.getQuantity();
            if (card == null)
                continue;
            if (token.cardRequestHasNoCode()) {
                List<Token> list = new ArrayList<>();
                tokenReplacers.put(token, list);
                continue;
            }
            int available = availableInventory.count(card);
            if (available <= 0) {
                List<Token> list = new ArrayList<>();
                tokenReplacers.put(token, list);
                continue;
            }
            int numTaken = Math.min(requestedAmount, available);
            availableInventory.remove(card, numTaken);
            if (available >= requestedAmount)
                continue;

            List<Token> list = new ArrayList<>();
            list.add(Token.LegalCard(card, numTaken, token.getTokenSection(), true));
            tokenReplacers.put(token, list);
        }
        if(tokenReplacers.isEmpty())
            return tokens; //We have every card that was requested.
        //Step 2: Try to find alternative prints for the ones that do not request an edition.
        int capacity = tokens.size();
        for(Map.Entry<Token, List<Token>> tokenReplacer : tokenReplacers.entrySet()) {
            Token token = tokenReplacer.getKey();
            DeckSection tokenSection = token.getTokenSection();
            List<Token> replacementList = tokenReplacer.getValue();
            PaperCard card = token.getCard();
            String cardName = card.getName();
            CardPool substitutes = availableInventory.getFilteredPool(c -> c.getName().equals(cardName));
            // stream().toList() causes crash on Android 8-13, use Collectors.toList()
            // ref: https://developer.android.com/reference/java/util/stream/Stream#toList()
            List<Map.Entry<PaperCard, Integer>> sortedSubstitutes = StreamUtil.stream(substitutes).sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
            int neededQuantity = token.getQuantity();
            for(Token found : replacementList) {
                //If there's an item in the replacement list already it means we've already found some of the needed copies.
                neededQuantity -= found.getQuantity();
            }
            for(int i = 0; i < sortedSubstitutes.size() && neededQuantity > 0; i++) {
                Map.Entry<PaperCard, Integer> item = sortedSubstitutes.get(i);
                PaperCard replacement = item.getKey();
                int toMove = Math.min(neededQuantity, item.getValue());
                replacementList.add(Token.LegalCard(replacement, toMove, tokenSection, true));
                availableInventory.remove(replacement, toMove);
                neededQuantity -= toMove;
                capacity++;
            }
            if(neededQuantity > 0) {
                PaperCard freePrint = getInfiniteSupplyPrinting(card);
                if(freePrint != null)
                    replacementList.add(Token.NotInInventoryFree(freePrint, neededQuantity, tokenSection));
                else
                    replacementList.add(Token.NotInInventory(card, neededQuantity, tokenSection));
                capacity++;
            }
        }
        //Step 3: Apply the replacement list.
        List<Token> newList = new ArrayList<>(capacity);
        for(Token t : this.tokens) {
            if(tokenReplacers.containsKey(t))
                newList.addAll(tokenReplacers.get(t));
            else
                newList.add(t);
        }
        this.tokens.clear();
        this.tokens.addAll(newList);
        return tokens;
    }

    private PaperCard getInfiniteSupplyPrinting(PaperCard card) {
        if(this.freePrintSupplier == null)
            return null;
        return freePrintSupplier.apply(card);
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
            cardKey = card.getUnFoiled();
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
        if (currentDeckName != null && !currentDeckName.trim().isEmpty())
            deckName = String.format("\"%s\"", currentDeckName.trim());

        String tokenDeckName = getTokenDeckNameIfAny();
        if (!tokenDeckName.isEmpty())
            tokenDeckName = String.format("\"%s\"", tokenDeckName);

        if(this.currentDeckNotEmpty) {
            final String warning;
            final String title;
            if (this.importBehavior == ImportBehavior.CREATE_NEW) {
                String extraWarning = localizer.getMessage("lblNewDeckWarning", deckName);
                warning = localizer.getMessage("lblConfirmCreateNewDeck", tokenDeckName, extraWarning);
                title = localizer.getMessage("lblNewDeckDialogTitle");
            } else if (this.importBehavior == ImportBehavior.MERGE){
                String extraWarning = (!tokenDeckName.isEmpty() && !tokenDeckName.equals(deckName)) ?
                        localizer.getMessage("lblCardImportWarning", deckName, tokenDeckName) : "";
                warning = localizer.getMessage("lblConfirmCardImport", deckName, extraWarning);
                title = localizer.getMessage("lblImportCardsDialogTitle");
            }
            else {
                warning = localizer.getMessage("lblConfirmReplaceDeck", deckName);
                title = localizer.getMessage("lblNewDeckDialogTitle");
            }
            if (!SOptionPane.showConfirmDialog(warning, title,
                    localizer.getMessage("lblYes"), localizer.getMessage("lblNo")))
                return null;
        }
        final Deck resultDeck = new Deck();
        for (final Token t : tokens) {
            final TokenType type = t.getType();
            // only Deck Name, legal card and limited card tokens will be analysed!
            if (!t.isTokenForDeck() ||
                    (type == TokenType.LIMITED_CARD && !this.includeBnRInDeck))
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

    public String getTokenMessage(DeckRecognizer.Token token) {
        return switch (token.getType()) {
            case LEGAL_CARD, LIMITED_CARD, CARD_FROM_NOT_ALLOWED_SET, CARD_FROM_INVALID_SET,
                 CARD_NOT_IN_INVENTORY, FREE_CARD_NOT_IN_INVENTORY ->
                    String.format("%s x %s %s", token.getQuantity(), token.getText(), getTokenFoilLabel(token));
            // Card Warning Msgs
            case UNKNOWN_CARD, UNSUPPORTED_CARD ->
                    token.getQuantity() > 0 ? String.format("%s x %s", token.getQuantity(), token.getText())
                            : token.getText();
            case UNSUPPORTED_DECK_SECTION ->
                    String.format("%s: %s", Localizer.getInstance().getMessage("lblWarningMsgPrefix"),
                            Localizer.getInstance()
                                    .getMessage("lblWarnDeckSectionNotAllowedInEditor", token.getText(),
                                            this.currentGameType.name()));

            // Special Case of Card moved into another section (e.g. Commander from Sideboard)
            case WARNING_MESSAGE -> String.format("%s: %s", Localizer.getInstance()
                    .getMessage("lblWarningMsgPrefix"), token.getText());

            // Placeholders
            case DECK_SECTION_NAME -> String.format("%s: %s", Localizer.getInstance().getMessage("lblDeckSection"),
                    token.getText());
            case CARD_RARITY -> String.format("%s: %s", Localizer.getInstance().getMessage("lblRarity"),
                    token.getText());
            case CARD_TYPE, CARD_CMC, MANA_COLOUR, COMMENT, UNKNOWN_TEXT -> token.getText();
            case DECK_NAME -> String.format("%s: %s", Localizer.getInstance().getMessage("lblDeckName"),
                    token.getText());
        };
    }

    public String getTokenStatusMessage(DeckRecognizer.Token token) {
        if (token == null)
            return "";

        final Localizer localizer = Localizer.getInstance();
        return switch (token.getType()) {
            case LIMITED_CARD -> String.format("%s: %s", localizer.getMessage("lblWarningMsgPrefix"),
                    localizer.getMessage("lblWarnLimitedCard",
                            StringUtils.capitalize(token.getLimitedCardType().name()), getGameFormatLabel()));
            case CARD_FROM_NOT_ALLOWED_SET ->
                    localizer.getMessage("lblErrNotAllowedCard", getGameFormatLabel());
            case CARD_FROM_INVALID_SET -> localizer.getMessage("lblErrCardEditionDate");
            case UNSUPPORTED_CARD -> localizer.getMessage("lblErrUnsupportedCard", this.currentGameType);
            case UNKNOWN_CARD -> String.format("%s: %s", localizer.getMessage("lblWarningMsgPrefix"),
                    localizer.getMessage("lblWarnUnknownCardMsg"));
            case CARD_NOT_IN_INVENTORY -> localizer.getMessage("lblWarnNotInInventory");
            default -> "";
        };
    }


    private String getTokenFoilLabel(DeckRecognizer.Token token) {
        if (!token.isCardToken())
            return "";
        final String foilMarker = "- (Foil)";
        return token.getCard().isFoil() ? foilMarker : "";
    }

    private String getGameFormatLabel() {
        return String.format("\"%s\"", this.getCurrentGameFormatName());
    }
}
