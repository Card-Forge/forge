package forge.deck;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.interfaces.ICheckBox;
import forge.interfaces.IComboBox;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.gui.SOptionPane;

public class DeckImportController {
    private final boolean replacingDeck;
    private final ICheckBox newEditionCheck, dateTimeCheck, onlyCoreExpCheck;
    private final IComboBox<String> monthDropdown;
    private final IComboBox<Integer> yearDropdown;
    private final List<DeckRecognizer.Token> tokens = new ArrayList<DeckRecognizer.Token>();

    public DeckImportController(boolean replacingDeck0, ICheckBox newEditionCheck0, ICheckBox dateTimeCheck0, ICheckBox onlyCoreExpCheck0, IComboBox<String> monthDropdown0, IComboBox<Integer> yearDropdown0) {
        replacingDeck = replacingDeck0;
        newEditionCheck = newEditionCheck0;
        dateTimeCheck = dateTimeCheck0;
        onlyCoreExpCheck = onlyCoreExpCheck0;
        monthDropdown = monthDropdown0;
        yearDropdown = yearDropdown0;

        fillDateDropdowns();
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

        DeckRecognizer recognizer = new DeckRecognizer(newEditionCheck.isSelected(),  onlyCoreExpCheck.isSelected(), FModel.getMagicDb().getCommonCards());
        if (dateTimeCheck.isSelected()) {
            recognizer.setDateConstraint(monthDropdown.getSelectedIndex(), yearDropdown.getSelectedItem());
        }
        String[] lines = input.split("\n");
        for (String line : lines) {
            tokens.add(recognizer.recognizeLine(line));
        }
        return tokens;
    }

    public Deck accept() {
        if (tokens.isEmpty()) { return null; }

        if (replacingDeck) {
            final String warning = "This will replace the contents of your current deck with these cards.\n\nProceed?";
            if (!SOptionPane.showConfirmDialog(warning, "Replace Current Deck", "Replace", "Cancel")) {
                return null;
            }
        }

        final Deck result = new Deck();
        boolean isMain = true;
        for (final DeckRecognizer.Token t : tokens) {
            final DeckRecognizer.TokenType type = t.getType();
            if ((type == DeckRecognizer.TokenType.SectionName) && t.getText().toLowerCase().contains("side")) {
                isMain = false;
            }
            if (type != DeckRecognizer.TokenType.KnownCard) {
                continue;
            }
            final PaperCard crd = t.getCard();
            if (isMain) {
                result.getMain().add(crd, t.getNumber());
            }
            else {
                result.getOrCreate(DeckSection.Sideboard).add(crd, t.getNumber());
            }
        }
        return result;
    }
}
