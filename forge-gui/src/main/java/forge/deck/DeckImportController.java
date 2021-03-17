package forge.deck;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import forge.gui.interfaces.ICheckBox;
import forge.gui.interfaces.IComboBox;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.Localizer;

public class DeckImportController {
    private final boolean replacingDeck;
    private final ICheckBox newEditionCheck, dateTimeCheck, onlyCoreExpCheck;
    private final IComboBox<String> monthDropdown;
    private final IComboBox<Integer> yearDropdown;
    private final List<DeckRecognizer.Token> tokens = new ArrayList<>();

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
        final Localizer localizer = Localizer.getInstance();
        if (tokens.isEmpty()) { return null; }

        if (replacingDeck) {
            final String warning = localizer.getMessage("lblReplaceCurrentDeckConfirm");
            if (!SOptionPane.showConfirmDialog(warning, localizer.getMessage("lblReplaceCurrentDeck"), localizer.getMessage("lblReplace"), localizer.getMessage("lblCancel"))) {
                return null;
            }
        }

        final Deck result = new Deck();
        DeckSection deckSection = DeckSection.Main;
        String section = "";
        for (final DeckRecognizer.Token t : tokens) {
            final DeckRecognizer.TokenType type = t.getType();
            if (type == DeckRecognizer.TokenType.SectionName) {
                section = t.getText().toLowerCase();
                if (section.startsWith("//")) {
                    continue;
                }
                // can't use wildcards in switch/case, so if/else it is
                if (section.startsWith("main")) {
                    deckSection = DeckSection.Main;
                }
                else if (section.startsWith("side")) {
                    deckSection = DeckSection.Sideboard;
                }
                else if (section.startsWith("commander")) {
                    deckSection = DeckSection.Commander;
                }
                else if (section.startsWith("avatar")) {
                    deckSection = DeckSection.Avatar;
                }
                else if (section.startsWith("planes")) {
                    deckSection = DeckSection.Planes;
                }
                else if (section.startsWith("scheme")) {
                    deckSection = DeckSection.Schemes;
                }
                else if (section.startsWith("conspiracy")) {
                    deckSection = DeckSection.Conspiracy;
                }
                else {
                    throw new NotImplementedException("Unexpected section: " + t.getText());
                }
            }
            if (type != DeckRecognizer.TokenType.KnownCard) {
                continue;
            }
            final PaperCard crd = t.getCard();
            result.getOrCreate(deckSection).add(crd, t.getNumber());
        }
        return result;
    }
}
