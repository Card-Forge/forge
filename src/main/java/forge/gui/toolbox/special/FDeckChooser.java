package forge.gui.toolbox.special;

import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.ArrayUtils;

import forge.Command;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.deck.generate.GenerateThemeDeck;
import forge.game.RegisteredPlayer;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.JXButtonPanel;
import forge.item.PreconDeck;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.quest.QuestEventChallenge;
import forge.quest.QuestUtil;
import forge.util.storage.IStorage;
import forge.util.storage.IStorageView;

@SuppressWarnings("serial")
public class FDeckChooser extends JPanel {
    private final JRadioButton radColors = new FRadioButton("Fully random color deck");
    private final JRadioButton radThemes = new FRadioButton("Semi-random theme deck");
    private final JRadioButton radCustom = new FRadioButton("Custom user deck");
    private final JRadioButton radQuests = new FRadioButton("Quest opponent deck");
    private final JRadioButton radPrecons = new FRadioButton("Decks from quest shop");

    private final JList<String> lstDecks  = new FList<String>();
    private final FLabel btnRandom = new FLabel.ButtonBuilder().text("Random").fontSize(16).build();
    private final FLabel btnChange = new FLabel.ButtonBuilder().text("Change player type").fontSize(16).build();
    
    private final JScrollPane scrDecks =
            new FScrollPane(lstDecks, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    private final FLabel lblDecklist = new FLabel.Builder().text("Double click any non-random deck for its decklist.").fontSize(12).build();

    private final JPanel pnlRadios = new JPanel(new MigLayout("insets 0, gap 0, wrap 2"));
    
    private final FLabel titleLabel;
    private final String titleTextTemplate;
    private final boolean canChoosePlayerType;
    private boolean isAi;
    
    private final MouseAdapter madDecklist = new MouseAdapter() {
        @SuppressWarnings("unchecked")
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (MouseEvent.BUTTON1 == e.getButton() && e.getClickCount() == 2) {
                final JList<String> src = ((JList<String>) e.getSource());
                if (getRadColors().isSelected() || getRadThemes().isSelected()) { return; }
                DeckgenUtil.showDecklist(src);
            }
        }
    };

    
    public FDeckChooser(final String titleText, boolean forAi, boolean canSwitchType) {
        setOpaque(false);
        isAi = forAi;
        titleTextTemplate = titleText;
        canChoosePlayerType = canSwitchType;
        titleLabel = new FLabel.Builder().text(titleText).fontStyle(Font.BOLD).fontSize(16).build();
        if( canChoosePlayerType )
            updateTitle();
    }
    
    private void updateTitle() {
        String title = canChoosePlayerType ? String.format(titleTextTemplate, isAi ? "AI" : "player's" ) : titleTextTemplate;
        titleLabel.setText(title);
    }

    public FDeckChooser(String titleText, boolean forAi) {
        this(titleText, forAi, false);
    }

    private void _listen(final JRadioButton btn, final Runnable onSelect) {
        btn.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (btn.isSelected()) { onSelect.run(); }
        }});
    }
    
    public void initialize() {
        // Radio button group
        final String strRadioConstraints = "h 28px!";
        JXButtonPanel grpRadios = new JXButtonPanel();
        grpRadios.add(radCustom, strRadioConstraints);
        grpRadios.add(radQuests, strRadioConstraints);
        grpRadios.add(radPrecons, strRadioConstraints);
        grpRadios.add(radColors, strRadioConstraints);
        grpRadios.add(radThemes, strRadioConstraints);

        pnlRadios.setOpaque(false);
        pnlRadios.add(titleLabel, canChoosePlayerType ? "split 2, sx 2, pushx, growx, h 28px!" : "sx 2");
        if(canChoosePlayerType) {
            //titleLabel.setHoverable(true);
            titleLabel.setOpaque(true);
            titleLabel.setSelected(true);
            //titleLabel.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME));
            //titleLabel.setBorder(null);
            pnlRadios.add(btnChange, "w 180px!, h 28px!, gap 10px 0 0 4px, ax right");
        }
        pnlRadios.add(grpRadios, "pushx, growx");
        pnlRadios.add(btnRandom, "w 180px!, h 30px!, gap 10px 0 0 0, ax center, ay bottom");
        
        // Radio button event handling
        _listen(getRadColors(), new Runnable() { @Override public void run() { updateColors();      } });
        _listen(getRadThemes(), new Runnable() { @Override public void run() { updateThemes();      } });
        _listen(getRadCustom(), new Runnable() { @Override public void run() { updateCustom();      } });
        _listen(getRadQuests(), new Runnable() { @Override public void run() { updateQuestEvents(); } });
        _listen(getRadPrecons(), new Runnable() { @Override public void run() { updatePrecons();    } });

        // First run: colors
        getRadColors().setSelected(true);
        
        btnChange.setCommand(new Command() {
            @Override public void run() { isAi = !isAi; updateTitle(); } });

    }


    private JList<String> getLstDecks()  { return lstDecks;  }
    private FLabel       getBtnRandom() { return btnRandom; }
    private JRadioButton getRadColors() { return radColors; }
    private JRadioButton getRadThemes() { return radThemes; }
    private JRadioButton getRadCustom() { return radCustom; }
    private JRadioButton getRadQuests() { return radQuests; }
    private JRadioButton getRadPrecons() { return radPrecons; }

    /** Handles all control for "colors" radio button click. */
    private void updateColors() {
        final JList<String> lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        lst.setListData(new String[] {"Random 1", "Random 2", "Random 3", "Black", "Blue", "Green", "Red", "White"});
        lst.setName(DeckgenUtil.DeckTypes.COLORS.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        getBtnRandom().setText("Random colors");
        getBtnRandom().setCommand(new Command() {
            @Override public void run() { lst.setSelectedIndices(DeckgenUtil.randomSelectColors(8)); } });

        // Init basic two color deck
        lst.setSelectedIndices(new int[]{0, 1});
    }

    /** Handles all control for "themes" radio button click. */
    private void updateThemes() {
        final JList<String> lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        final List<String> themeNames = new ArrayList<String>();
        for (final String s : GenerateThemeDeck.getThemeNames()) { themeNames.add(s); }

        lst.setListData(themeNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(DeckgenUtil.DeckTypes.THEMES.toString());
        lst.removeMouseListener(madDecklist);

        getBtnRandom().setText("Random deck");
        getBtnRandom().setCommand(new Command() {
            @Override public void run() { DeckgenUtil.randomSelect(lst); } });

        // Init first in list
        lst.setSelectedIndex(0);
    }

    /** Handles all control for "custom" radio button click. */
    private void updateCustom() {
        final JList<String> lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final List<String> customNames = new ArrayList<String>();
        final IStorage<Deck> allDecks = Singletons.getModel().getDecks().getConstructed();
        for (final Deck d : allDecks) { customNames.add(d.getName()); }

        lst.setListData(customNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(DeckgenUtil.DeckTypes.CUSTOM.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        getBtnRandom().setText("Random deck");
        getBtnRandom().setCommand(new Command() {
            @Override public void run() { DeckgenUtil.randomSelect(lst); } });

        // Init first in list
        lst.setSelectedIndex(0);
    }

    /** Handles all control for "custom" radio button click. */
    private void updatePrecons() {
        final JList<String> lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final List<String> customNames = new ArrayList<String>();
        final IStorageView<PreconDeck> allDecks = QuestController.getPrecons();
        for (final PreconDeck d : allDecks) { customNames.add(d.getName()); }

        lst.setListData(customNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(DeckgenUtil.DeckTypes.PRECON.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        getBtnRandom().setText("Random deck");
        getBtnRandom().setCommand(new Command() {
            @Override public void run() { DeckgenUtil.randomSelect(lst); } });

        // Init first in list
        lst.setSelectedIndex(0);
    }
    
    /** Handles all control for "quest event" radio button click. */
    private void updateQuestEvents() {
        final JList<String> lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final List<String> eventNames = new ArrayList<String>();

        QuestController quest = Singletons.getModel().getQuest();
        for (QuestEvent e : quest.getDuelsManager().getAllDuels()) {
            eventNames.add(e.getName());
        }

        for (QuestEvent e : quest.getChallenges()) {
            eventNames.add(e.getTitle());
        }

        lst.setListData(eventNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(DeckgenUtil.DeckTypes.QUESTEVENTS.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        getBtnRandom().setText("Random event");
        getBtnRandom().setCommand(new Command() {
            @Override public void run() { DeckgenUtil.randomSelect(lst); } });

        // Init first in list
        lst.setSelectedIndex(0);
    }

    /** Generates deck from current list selection(s). */
    public RegisteredPlayer getDeck() {

        
        JList<String> lst0 = getLstDecks();
        final List<String> selection = lst0.getSelectedValuesList();

        if (selection.isEmpty()) { return null; }

        // Special branch for quest events
        if (lst0.getName().equals(DeckgenUtil.DeckTypes.QUESTEVENTS.toString())) {
            QuestEvent event = DeckgenUtil.getQuestEvent(selection.get(0)); 
            RegisteredPlayer result = new RegisteredPlayer(event.getEventDeck());
            if( event instanceof QuestEventChallenge ) {
                result.setStartingLife(((QuestEventChallenge) event).getAiLife());
            }
            result.setCardsOnBattlefield(QuestUtil.getComputerStartingCards(event));
            return result;
        }

        Deck deck = null;
        if (lst0.getName().equals(DeckgenUtil.DeckTypes.COLORS.toString()) && DeckgenUtil.colorCheck(selection)) {
            deck = DeckgenUtil.buildColorDeck(selection, isAi);
        } else if (lst0.getName().equals(DeckgenUtil.DeckTypes.THEMES.toString())) {
            deck = DeckgenUtil.buildThemeDeck(selection.get(0));
        } else if (lst0.getName().equals(DeckgenUtil.DeckTypes.CUSTOM.toString())) {
            deck = DeckgenUtil.getConstructedDeck(selection.get(0));
        } else if (lst0.getName().equals(DeckgenUtil.DeckTypes.PRECON.toString())) {
            deck = DeckgenUtil.getPreconDeck(selection.get(0));
        }

        return RegisteredPlayer.fromDeck(deck);
    }


    public final boolean isAi() {
        return isAi;
    }

    public void populate() {
        this.setLayout(new MigLayout("insets 0, gap 0, flowy, ax right"));

        this.add(pnlRadios, "w 100%!, gap 0 0 0 12px");
        this.add(scrDecks, "w 100%!, growy, pushy");
        this.add(lblDecklist, "w 100%!, h 20px!");
    }
}
