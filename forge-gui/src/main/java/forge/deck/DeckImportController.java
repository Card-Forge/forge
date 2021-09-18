package forge.deck;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import forge.StaticData;
import forge.card.CardDb;
import forge.game.GameFormat;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import org.apache.commons.lang3.StringUtils;

import forge.gui.interfaces.ICheckBox;
import forge.gui.interfaces.IComboBox;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.util.Localizer;

public class DeckImportController {
    private boolean createNewDeck;
    // Date filter
    private final ICheckBox dateTimeCheck;
    private final IComboBox<String> monthDropdown;
    private final IComboBox<Integer> yearDropdown;
    // CardArt Preference Filter
    private CardDb.CardArtPreference artPreference;
    // Block Preference Filter
    private ICheckBox blockCheck = null;
    private IComboBox<GameFormat> blocksDropdown = null;
    private boolean isAnyBlockFormatSupported = false;

    private final List<DeckRecognizer.Token> tokens = new ArrayList<>();
    private final boolean currentDeckNotEmpty;
    private final List<String> allowedSetCodes;
    private final DeckFormat currentDeckFormat;

    public DeckImportController(ICheckBox dateTimeCheck0,
                                IComboBox<String> monthDropdown0, IComboBox<Integer> yearDropdown0,
                                boolean currentDeckNotEmpty) {
        this(dateTimeCheck0, monthDropdown0, yearDropdown0, currentDeckNotEmpty, null, null,
                null, null);
    }

    public DeckImportController(ICheckBox dateTimeCheck0,
                                IComboBox<String> monthDropdown0, IComboBox<Integer> yearDropdown0,
                                boolean currentDeckNotEmpty, List<String> setCodes, DeckFormat deckFormat,
                                ICheckBox blockCheck0, IComboBox<GameFormat> blocksDropdown) {
        this.dateTimeCheck = dateTimeCheck0;
        this.monthDropdown = monthDropdown0;
        this.yearDropdown = yearDropdown0;
        this.artPreference = StaticData.instance().getCardArtPreference();  // default
        if (blockCheck0 != null && blocksDropdown != null){
            this.blockCheck = blockCheck0;
            this.blocksDropdown = blocksDropdown;
        }

        /* This keeps track whether the current deck in editor is **not empty**.
           If that is the case, and the "Replace" option won't be checked, this will
           allow to ask for confirmation whether the *Merge* action is what
           really intended by the user!
         */
        this.currentDeckNotEmpty = currentDeckNotEmpty;
        // this option will control the "new deck" action controlled by UI widget
        createNewDeck = false;
        if (setCodes != null && setCodes.size() == 0)
            this.allowedSetCodes = null;
        else
            this.allowedSetCodes = setCodes;
        this.currentDeckFormat = deckFormat;

        fillDateDropdowns();
    }

    public void setCardArtPreference(boolean isLatest, boolean coreFilterEnabled){
        this.artPreference = StaticData.instance().getCardArtPreference(isLatest, coreFilterEnabled);
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

        if (this.blocksDropdown != null &&
                FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.LOAD_HISTORIC_FORMATS)){
            this.blocksDropdown.removeAllItems();
            final List<GameFormat> blockFormats = FModel.getBlockFormats();
            for (final GameFormat f : blockFormats) {
                if (isBlockFormatCompliantWithCurrentGameType(f)) {
                    this.blocksDropdown.addItem(f);
                    this.isAnyBlockFormatSupported = true;
                }
            }
        }
    }

    private boolean isBlockFormatCompliantWithCurrentGameType(GameFormat f) {
        if (this.allowedSetCodes == null)
            return true;
        List<String> formatSetCodes = f.getAllowedSetCodes();
        boolean blockFormatCompliant = true;
        for (String setCode : formatSetCodes){
            if (!allowedSetCodes.contains(setCode)){
                blockFormatCompliant = false;
                break;
            }
        }
        return blockFormatCompliant;
    }

    public boolean isBlockFormatsSupported() {
        return this.isAnyBlockFormatSupported;
    }

    public List<DeckRecognizer.Token> parseInput(String input) {
        tokens.clear();
        DeckRecognizer recognizer = new DeckRecognizer();
        // Set Art Preference first thing
        recognizer.setArtPreference(this.artPreference);

        if (dateTimeCheck.isSelected())
            recognizer.setDateConstraint(yearDropdown.getSelectedItem(), monthDropdown.getSelectedIndex());

        if (this.allowedSetCodes != null && this.allowedSetCodes.size() > 0){
            if (this.blockCheck.isSelected() && this.isAnyBlockFormatSupported) {
                GameFormat gameFormat = this.blocksDropdown.getSelectedItem();
                recognizer.setGameFormatConstraint(gameFormat.getAllowedSetCodes());
            } else
                recognizer.setGameFormatConstraint(this.allowedSetCodes);
        }

        if (this.currentDeckFormat != null)
            recognizer.setDeckFormatConstraint(this.currentDeckFormat);

        String[] lines = input.split("\n");
        DeckSection referenceDeckSectionInParsing = null;  // default
        for (String line : lines) {
            DeckRecognizer.Token token = recognizer.recognizeLine(line, referenceDeckSectionInParsing);
            if (token != null) {
                if (token.getType() == DeckRecognizer.TokenType.DECK_SECTION_NAME)
                    referenceDeckSectionInParsing = DeckSection.valueOf(token.getText());
                else if (token.getType() == DeckRecognizer.TokenType.LEGAL_CARD_REQUEST) {
                    DeckSection tokenSection = token.getTokenSection();
                    if (!tokenSection.equals(referenceDeckSectionInParsing)) {
                        DeckRecognizer.Token sectionToken = DeckRecognizer.Token.DeckSection(token.getTokenSection().name());
                        if (referenceDeckSectionInParsing == null)
                            tokens.add(0, sectionToken);  // first ever - put on top!
                        else
                            tokens.add(sectionToken);  // add just before card token
                        referenceDeckSectionInParsing = tokenSection;
                    }
                }
                if (token.getType() == DeckRecognizer.TokenType.DECK_NAME)
                    tokens.add(0, token);  // always add deck name top of the decklist
                else
                    tokens.add(token);
            }

        }
        return tokens;
    }

    public Deck accept(){
        return this.accept("");
    }

    public Deck accept(String currentDeckName) {
        final Localizer localizer = Localizer.getInstance();
        if (tokens.isEmpty()) { return null; }

        String deckName = "";
        if (currentDeckName != null && currentDeckName.length() > 0)
            deckName = String.format("\"%s\"", currentDeckName);

        if (createNewDeck){
            String extraWarning = this.currentDeckNotEmpty ? localizer.getMessage("lblNewDeckWarning") : "";
            final String warning = localizer.getMessage("lblConfirmCreateNewDeck", deckName, extraWarning);
            if (!SOptionPane.showConfirmDialog(warning, localizer.getMessage("lblNewDeckDialogTitle"),
                    localizer.getMessage("lblYes"), localizer.getMessage("lblNo"))) {
                return null;
            }
        }
        else if (this.currentDeckNotEmpty){
            final String warning = localizer.getMessage("lblConfirmCardImport", deckName);
            if (!SOptionPane.showConfirmDialog(warning,
                    localizer.getMessage("lblImportCardsDialogTitle"),
                    localizer.getMessage("lblYes"), localizer.getMessage("lblNo")))
                return null;
        }
        final Deck resultDeck = new Deck();
        DeckSection deckSection = DeckSection.Main;
        for (final DeckRecognizer.Token t : tokens) {
            final DeckRecognizer.TokenType type = t.getType();

            if (type == DeckRecognizer.TokenType.DECK_NAME) {
                resultDeck.setName(t.getText());
            }
            if (type == DeckRecognizer.TokenType.DECK_SECTION_NAME) {
                deckSection = DeckSection.smartValueOf(t.getText());
            }
            // all other tokens will be discarded.
            if (type != DeckRecognizer.TokenType.LEGAL_CARD_REQUEST) {
                continue;
            }
            final PaperCard crd = t.getCard();
            // Leverage on the DeckSection validation mechanism to match cards to the corresponding deck section!
            if (deckSection.validate(crd))
                resultDeck.getOrCreate(deckSection).add(crd, t.getNumber());
            else {
                DeckSection matchingSec = DeckSection.matchingSection(crd);
                resultDeck.getOrCreate(matchingSec).add(crd, t.getNumber());
            }
        }
        return resultDeck;
    }
}
