package forge.gui.toolbox;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import org.apache.commons.lang3.ArrayUtils;

import net.miginfocom.swing.MigLayout;

import forge.Command;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.deck.generate.GenerateThemeDeck;
import forge.game.player.PlayerType;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.util.IStorage;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class FDeckChooser extends JPanel {

    private enum ESubmenuConstructedTypes { /** */
        COLORS, /** */
        THEMES, /** */
        CUSTOM, /** */
        QUESTEVENTS
    }
    
    private final QuestController quest = Singletons.getModel().getQuest();
    
    private final JRadioButton radColors = new FRadioButton("Fully random color deck");
    private final JRadioButton radThemes = new FRadioButton("Semi-random theme deck");
    private final JRadioButton radCustom = new FRadioButton("Custom user deck");
    private final JRadioButton radQuests = new FRadioButton("Quest opponent deck");
 
    private final JList lstDecks      = new FList();
    private final ExperimentalLabel btnRandom = new ExperimentalLabel("Random");

    private final JScrollPane scrDecks  = new FScrollPane(lstDecks, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    
    private final FLabel lblDecklist = new FLabel.Builder()
    .text("Double click a non-random deck for its decklist.")
    .fontSize(12).build();
    
    private final JPanel pnlRadios = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
    private final PlayerType playerType;
        

    private final MouseAdapter madDecklist = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.getClickCount() == 2) {
                final JList src = ((JList) e.getSource());
                    if (getRadColors().isSelected()) { return; }
                    if (getRadThemes().isSelected()) { return; }
                DeckgenUtil.showDecklist(src);
            }
        }
    };    
    
    public FDeckChooser(String titleText, PlayerType pt)
    {
        playerType = pt;
        
        // Radio button panels: Human and AI
        final String strRadioConstraints = "w 100%!, h 30px!";
        
        this.setOpaque(false);
        
        // Radio button group: Human
        final ButtonGroup grpRadios = new ButtonGroup();
        grpRadios.add(radCustom);
        grpRadios.add(radQuests);
        grpRadios.add(radColors);
        grpRadios.add(radThemes);
        
        pnlRadios.setOpaque(false);
        pnlRadios.add(new FLabel.Builder().text(titleText)
                .fontStyle(Font.BOLD).fontSize(16)
                .fontAlign(SwingConstants.LEFT).build(), strRadioConstraints);
        pnlRadios.add(lblDecklist, "h 20px!, gap 0 0 0 10px");
        pnlRadios.add(radCustom, strRadioConstraints);
        pnlRadios.add(radQuests, strRadioConstraints);
        pnlRadios.add(radColors, strRadioConstraints);
        pnlRadios.add(radThemes, strRadioConstraints);
        pnlRadios.add(btnRandom, "w 200px!, h 30px!, gap 0 0 10px 0, ax center");        

    }
    
    public void initialize() {
        // Radio button event handling
        getRadColors().addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) {
                updateColors(); } });

        getRadThemes().addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) {
                updateThemes(); } });

        getRadCustom().addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) {
                updateCustom(); } });

        getRadQuests().addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) {
                updateQuestEvents(); } });
        
        // First run: colors
        getRadColors().setSelected(true);
        updateColors();
    }
    
    
    /** @return {@link javax.swing.JList} */
    public JList getLstDecks() {
        return this.lstDecks;
    }
    /** @return {@link forge.gui.toolbox.ExperimentalLabel} */
    public ExperimentalLabel getBtnRandom() {
        return this.btnRandom;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadColors() {
        return this.radColors;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadThemes() {
        return this.radThemes;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadCustom() {
        return this.radCustom;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadQuests() {
        return this.radQuests;
    }

    /** Handles all control for "colors" radio button click. */
    private void updateColors() {
        final JList lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        lst.setListData(new String[] {"Random 1", "Random 2", "Random 3",
                "Random 4", "Black", "Blue", "Green", "Red", "White"});
        lst.setName(ESubmenuConstructedTypes.COLORS.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        final ExperimentalLabel btn = getBtnRandom();

        btn.setCommand(new Command() { @Override public void execute() { lst.setSelectedIndices( DeckgenUtil.randomSelectColors()); } });

        // Init basic two color deck
        lst.setSelectedIndices(new int[]{0, 1});
    }

    /** Handles all control for "themes" radio button click. */
    private void updateThemes() {
        final JList lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        lst.setName(ESubmenuConstructedTypes.COLORS.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        final List<String> themeNames = new ArrayList<String>();
        for (final String s : GenerateThemeDeck.getThemeNames()) { themeNames.add(s); }

        lst.setListData(themeNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(ESubmenuConstructedTypes.THEMES.toString());
        lst.removeMouseListener(madDecklist);

        final ExperimentalLabel btn = getBtnRandom();
        btn.setCommand(new Command() { @Override public void execute() { DeckgenUtil.randomSelect(lst); } });

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
        lst.setName(ESubmenuConstructedTypes.CUSTOM.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        final ExperimentalLabel btn = getBtnRandom();

        btn.setCommand(new Command() { @Override public void execute() { DeckgenUtil.randomSelect(lst); } });

        // Init first in list
        lst.setSelectedIndex(0);
    }

    /** Handles all control for "quest event" radio button click. */
    private void updateQuestEvents() {
        final JList lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final List<String> eventNames = new ArrayList<String>();

        for (final QuestEvent e : quest.getDuelsManager().getAllDuels()) {
            eventNames.add(e.getEventDeck().getName());
        }

        for (final QuestEvent e : quest.getChallengesManager().getAllChallenges()) {
            eventNames.add(e.getEventDeck().getName());
        }

        lst.setListData(eventNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(ESubmenuConstructedTypes.QUESTEVENTS.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        final ExperimentalLabel btn = getBtnRandom();
        btn.setCommand(new Command() { @Override public void execute() { DeckgenUtil.randomSelect(lst); } });

        // Init first in list
        lst.setSelectedIndex(0);
    }    
    
    /** Generates deck from current list selection(s). */
    public Deck getDeck() {
        JList lst0 = getLstDecks();
        final String[] selection = Arrays.copyOf(lst0.getSelectedValues(), lst0.getSelectedValues().length, String[].class);

        final Deck deck;

        if (selection.length == 0) { return null; }

        if (lst0.getName().equals(ESubmenuConstructedTypes.COLORS.toString()) && DeckgenUtil.colorCheck(selection)) {
            deck = DeckgenUtil.buildColorDeck(selection, getPlayerType());
        }
        else if (lst0.getName().equals(ESubmenuConstructedTypes.THEMES.toString())) {
            deck = DeckgenUtil.buildThemeDeck(selection);
        }
        else if (lst0.getName().equals(ESubmenuConstructedTypes.QUESTEVENTS.toString())) {
            deck = DeckgenUtil.buildQuestDeck(selection);
        }
        // Custom deck
        else if (lst0.getName().equals(ESubmenuConstructedTypes.CUSTOM.toString())) {
            deck = DeckgenUtil.buildCustomDeck(selection);
        }
        // Failure, for some reason
        else {
            deck = null;
        }

        return deck;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    private PlayerType getPlayerType() {
        return playerType;
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void populate() {
        
        this.setLayout(new MigLayout("insets 0, gap 0, flowy, ax right"));
        
        this.add(pnlRadios, "w 100%!, gap 0 0 20px 20px");
        this.add(scrDecks, "w 100%!, growy, pushy");
                
    }    
    
}
