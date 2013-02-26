package forge.gui.toolbox;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.ArrayUtils;

import forge.Command;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.deck.generate.GenerateThemeDeck;
import forge.game.player.PlayerType;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.util.storage.IStorage;

@SuppressWarnings("serial")
public class FDeckChooser extends JPanel {
    private final JRadioButton radColors = new FRadioButton("Fully random color deck");
    private final JRadioButton radThemes = new FRadioButton("Semi-random theme deck");
    private final JRadioButton radCustom = new FRadioButton("Custom user deck");
    private final JRadioButton radQuests = new FRadioButton("Quest opponent deck");

    private final JList  lstDecks  = new FList();
    private final FLabel btnRandom = new FLabel.ButtonBuilder().text("Random").fontSize(16).build();

    private final JScrollPane scrDecks =
            new FScrollPane(lstDecks, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    private final FLabel lblDecklist = new FLabel.Builder().text("Double click a non-random deck for its decklist.").fontSize(12).build();

    private final JPanel pnlRadios = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
    private final PlayerType playerType;

    private final MouseAdapter madDecklist = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (MouseEvent.BUTTON1 == e.getButton() && e.getClickCount() == 2) {
                final JList src = ((JList) e.getSource());
                if (getRadColors().isSelected() || getRadThemes().isSelected()) { return; }
                DeckgenUtil.showDecklist(src);
            }
        }
    };

    public FDeckChooser(String titleText, PlayerType pt) {
        setOpaque(false);
        playerType = pt;

        // Radio button group
        final String strRadioConstraints = "w 100%!, h 30px!";
        JXButtonPanel grpRadios = new JXButtonPanel();
        grpRadios.add(radCustom, strRadioConstraints);
        grpRadios.add(radQuests, strRadioConstraints);
        grpRadios.add(radColors, strRadioConstraints);
        grpRadios.add(radThemes, strRadioConstraints);

        pnlRadios.setOpaque(false);
        pnlRadios.add(new FLabel.Builder().text(titleText).fontStyle(Font.BOLD).fontSize(16).build());
        pnlRadios.add(lblDecklist, "h 20px!, gap 0 0 0 10px");
        pnlRadios.add(grpRadios, "w 100%");
        pnlRadios.add(btnRandom, "w 200px!, h 30px!, gap 0 0 10px 0, ax center");
    }

    private void _listen(final JRadioButton btn, final Runnable onSelect) {
        btn.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                if (btn.isSelected()) { onSelect.run(); }
            }
        });
    }
    
    public void initialize() {
        // Radio button event handling
        _listen(getRadColors(), new Runnable() { @Override public void run() { updateColors();      } });
        _listen(getRadThemes(), new Runnable() { @Override public void run() { updateThemes();      } });
        _listen(getRadCustom(), new Runnable() { @Override public void run() { updateCustom();      } });
        _listen(getRadQuests(), new Runnable() { @Override public void run() { updateQuestEvents(); } });

        // First run: colors
        getRadColors().setSelected(true);
    }


    public JList        getLstDecks()  { return lstDecks;  }
    public FLabel       getBtnRandom() { return btnRandom; }
    public JRadioButton getRadColors() { return radColors; }
    public JRadioButton getRadThemes() { return radThemes; }
    public JRadioButton getRadCustom() { return radCustom; }
    public JRadioButton getRadQuests() { return radQuests; }

    /** Handles all control for "colors" radio button click. */
    private void updateColors() {
        final JList lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        lst.setListData(new String[] {"Random 1", "Random 2", "Random 3",
                "Random 4", "Black", "Blue", "Green", "Red", "White"});
        lst.setName(DeckgenUtil.DeckTypes.COLORS.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        getBtnRandom().setCommand(new Command() {
            @Override public void execute() { lst.setSelectedIndices(DeckgenUtil.randomSelectColors()); } });

        // Init basic two color deck
        lst.setSelectedIndices(new int[]{0, 1});
    }

    /** Handles all control for "themes" radio button click. */
    private void updateThemes() {
        final JList lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        final List<String> themeNames = new ArrayList<String>();
        for (final String s : GenerateThemeDeck.getThemeNames()) { themeNames.add(s); }

        lst.setListData(themeNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(DeckgenUtil.DeckTypes.THEMES.toString());
        lst.removeMouseListener(madDecklist);

        getBtnRandom().setCommand(new Command() {
            @Override public void execute() { DeckgenUtil.randomSelect(lst); } });

        // Init first in list
        lst.setSelectedIndex(0);
    }

    /** Handles all control for "custom" radio button click. */
    private void updateCustom() {
        final JList lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final List<String> customNames = new ArrayList<String>();
        final IStorage<Deck> allDecks = Singletons.getModel().getDecks().getConstructed();
        for (final Deck d : allDecks) { customNames.add(d.getName()); }

        lst.setListData(customNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(DeckgenUtil.DeckTypes.CUSTOM.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        getBtnRandom().setCommand(new Command() {
            @Override public void execute() { DeckgenUtil.randomSelect(lst); } });

        // Init first in list
        lst.setSelectedIndex(0);
    }

    /** Handles all control for "quest event" radio button click. */
    private void updateQuestEvents() {
        final JList lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final List<String> eventNames = new ArrayList<String>();

        QuestController quest = Singletons.getModel().getQuest();
        for (QuestEvent e : quest.getDuelsManager().getAllDuels()) {
            eventNames.add(e.getEventDeck().getName());
        }

        for (QuestEvent e : quest.getChallengesManager().getAllChallenges()) {
            eventNames.add(e.getEventDeck().getName());
        }

        lst.setListData(eventNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(DeckgenUtil.DeckTypes.QUESTEVENTS.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        getBtnRandom().setCommand(new Command() {
            @Override public void execute() { DeckgenUtil.randomSelect(lst); } });

        // Init first in list
        lst.setSelectedIndex(0);
    }

    /** Generates deck from current list selection(s). */
    public Deck getDeck() {
        JList lst0 = getLstDecks();
        final String[] selection = Arrays.copyOf(lst0.getSelectedValues(), lst0.getSelectedValues().length, String[].class);

        if (selection.length == 0) { return null; }

        if (lst0.getName().equals(DeckgenUtil.DeckTypes.COLORS.toString()) && DeckgenUtil.colorCheck(selection)) {
            return DeckgenUtil.buildColorDeck(selection, getPlayerType());
        } else if (lst0.getName().equals(DeckgenUtil.DeckTypes.THEMES.toString())) {
            return DeckgenUtil.buildThemeDeck(selection);
        } else if (lst0.getName().equals(DeckgenUtil.DeckTypes.QUESTEVENTS.toString())) {
            return DeckgenUtil.buildQuestDeck(selection);
        } else if (lst0.getName().equals(DeckgenUtil.DeckTypes.CUSTOM.toString())) {
            return DeckgenUtil.getConstructedDeck(selection);
        }

        // Failure, for some reason
        return null;
    }

    private PlayerType getPlayerType() {
        return playerType;
    }

    public void populate() {
        this.setLayout(new MigLayout("insets 0, gap 0, flowy, ax right"));

        this.add(pnlRadios, "w 100%!, gap 0 0 20px 20px");
        this.add(scrDecks, "w 100%!, growy, pushy");
    }
}
