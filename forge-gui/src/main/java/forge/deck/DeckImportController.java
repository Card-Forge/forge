package forge.deck;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.gui.interfaces.ICheckBox;
import forge.gui.interfaces.IComboBox;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.Localizer;

public class DeckImportController {
    private boolean replacingDeck;
    private final ICheckBox dateTimeCheck;
    private final IComboBox<String> monthDropdown;
    private final IComboBox<Integer> yearDropdown;
    private final List<DeckRecognizer.Token> tokens = new ArrayList<>();
    private final boolean currentDeckNotEmpty;
    private List<String> allowedSetCodes;
    private DeckFormat currentDeckFormat;

    public DeckImportController(ICheckBox dateTimeCheck0,
                                IComboBox<String> monthDropdown0, IComboBox<Integer> yearDropdown0,
                                boolean currentDeckNotEmpty) {
        this(dateTimeCheck0, monthDropdown0, yearDropdown0, currentDeckNotEmpty, null, null);
    }

    public DeckImportController (ICheckBox dateTimeCheck0,
                                IComboBox<String> monthDropdown0, IComboBox<Integer> yearDropdown0,
                                boolean currentDeckNotEmpty, List<String> setCodes, DeckFormat deckFormat) {
        dateTimeCheck = dateTimeCheck0;
        monthDropdown = monthDropdown0;
        yearDropdown = yearDropdown0;

        /* This keeps track whether the current deck in editor is **not empty**.
           If that is the case, and the "Replace" option won't be checked, this will
           allow to ask for confirmation whether the *Merge* action is what
           really intended by the user!
         */
        this.currentDeckNotEmpty = currentDeckNotEmpty;
        // this option will control the "replace" action controlled by UI widget.
        replacingDeck = false; // by default is false, as we DO NOT want to replace by default.

        if (setCodes != null && setCodes.size() == 0)
            this.allowedSetCodes = null;
        else
            this.allowedSetCodes = setCodes;
        this.currentDeckFormat = deckFormat;
        fillDateDropdowns();
    }

    public void setReplacingDeck(boolean replaceDeck){
        this.replacingDeck = replaceDeck;
    }

    public boolean getReplacingDeck(){
        return this.replacingDeck;
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
            yearDropdown.addItem(Integer.valueOf(i));
        }
    }

    public List<DeckRecognizer.Token> parseInput(String input) {
        tokens.clear();
        DeckRecognizer recognizer = new DeckRecognizer(FModel.getMagicDb().getCommonCards(),
                                                       FModel.getMagicDb().getVariantCards());
        if (dateTimeCheck.isSelected()) {
            recognizer.setDateConstraint(yearDropdown.getSelectedItem(), monthDropdown.getSelectedIndex());
        }
        if (this.allowedSetCodes != null && this.allowedSetCodes.size() > 0)
            recognizer.setGameFormatConstraint(this.allowedSetCodes);
        if (this.currentDeckFormat != null)
            recognizer.setDeckFormatConstraint(this.currentDeckFormat);

        String[] lines = input.split("\n");
        for (String line : lines) {
            DeckRecognizer.Token token = recognizer.recognizeLine(line);
            if (token != null)
                tokens.add(token);
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

        if (replacingDeck) {
            final String warning = localizer.getMessage("lblConfirmReplaceDeck", deckName);
            if (!SOptionPane.showConfirmDialog(warning, localizer.getMessage("lblReplaceCurrentDeck"),
                    localizer.getMessage("lblYes"), localizer.getMessage("lblNo"))) {
                return null;
            }
        }  else if (this.currentDeckNotEmpty){
            final String warning = localizer.getMessage("lblConfirmCardImport", deckName);
            if (!SOptionPane.showConfirmDialog(warning,
                    localizer.getMessage("lblBulkImport"),
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
