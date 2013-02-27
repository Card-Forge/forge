package forge.gui.home.variant;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import net.miginfocom.swing.MigLayout;
import forge.Singletons;
import forge.deck.Deck;
import forge.game.player.PlayerType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.LblHeader;
import forge.gui.home.StartButton;
import forge.gui.home.VHomeUI;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FDeckChooser;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FTabbedPane;
import forge.gui.toolbox.JXButtonPanel;

/** 
 * Assembles Swing components of constructed submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuArchenemy implements IVSubmenu<CSubmenuArchenemy> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Archenemy Mode");

    /** */
    private final LblHeader lblTitle = new LblHeader("Variant: Archenemy");

    private final JPanel pnlStart = new JPanel(new MigLayout("insets 0, gap 0, wrap 2"));

    private final StartButton btnStart  = new StartButton();

    private final JCheckBox cbSingletons = new FCheckBox("Singleton Mode");
    private final JCheckBox cbArtifacts = new FCheckBox("Remove Artifacts");
    private final JCheckBox cbRemoveSmall = new FCheckBox("Remove Small Creatures");

    //////////////////////////////

    private final FLabel lblEditor = new FLabel.ButtonBuilder().text("Scheme Deck Editor").fontSize(16).build();
    private final FTabbedPane tabPane = new FTabbedPane();
    private final List<FPanel> playerPanels = new ArrayList<FPanel>();
    private final List<FDeckChooser> deckChoosers = new ArrayList<FDeckChooser>();
    private final FList archenemySchemes = new FList();
    private final List<Deck> allSchemeDecks = new ArrayList<Deck>();
    private final JCheckBox cbUseDefaultSchemes = new FCheckBox("Use default scheme decks if possible.");
    private final List<JRadioButton> fieldRadios = new ArrayList<JRadioButton>();
    private int currentNumTabsShown = 8;

    //////////////////////////////

    private VSubmenuArchenemy() {

        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        //This listener will look for any of the radio buttons being selected
        //and call the method that shows/hides tabs appropriately.
        ItemListener iListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0) {
                FRadioButton aButton = (FRadioButton) arg0.getSource();

                if (arg0.getStateChange() == ItemEvent.SELECTED) {
                    changeTabs(Integer.parseInt(aButton.getText()));
                }
            }

        };

        //Settings panel
        FPanel settingsPanel = new FPanel();
        settingsPanel.setLayout(new MigLayout("wrap 2, ax center"));
        FPanel radioPaneContainer = new FPanel(new MigLayout());
        radioPaneContainer.setOpaque(false);
        JXButtonPanel radioPane = new JXButtonPanel();
        radioPane.add(new FLabel.Builder().text("Set number of opponents").build());
        for (int i = 1; i < 8; i++) {
            FRadioButton tempRadio = new FRadioButton(String.valueOf(i));
            fieldRadios.add(tempRadio);
            tempRadio.addItemListener(iListener);
            radioPane.add(tempRadio, "align 50% 50%, gaptop 5");
        }
        radioPaneContainer.add(radioPane);
        settingsPanel.add(radioPaneContainer, "span 1 2");
        settingsPanel.add(cbUseDefaultSchemes);
        settingsPanel.add(lblEditor, "w pref + 24, h pref + 8, ax center");
        tabPane.add("Settings", settingsPanel);

        for (Deck schemeDeck : Singletons.getModel().getDecks().getScheme()) {
            if (!allSchemeDecks.contains(schemeDeck)) {
                allSchemeDecks.add(schemeDeck);
            }
        }

        //Player panels (Human + 7 AIs)
        for (int i = 0; i < 8; i++) {
            FPanel tempPanel = new FPanel();
            tempPanel.setLayout(new MigLayout("insets 0, gap 0 , wrap 2, flowy, ax center"));

            FDeckChooser tempChooser = new FDeckChooser("Select deck:", i == 0 ? PlayerType.HUMAN : PlayerType.COMPUTER);
            tempChooser.initialize();

            deckChoosers.add(tempChooser);

            tempPanel.add(tempChooser, "span 1 2, w 44%!, gap 0 0 20px 20px, growy, pushy, wrap");
            if (i == 0) {

                tempPanel.add(new FLabel.Builder().text("Select Scheme deck:").build(), "flowy");

                archenemySchemes.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

                JScrollPane scrSchemes = new FScrollPane(archenemySchemes, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                tempPanel.add(scrSchemes, "h 90%!,wrap");
            }

            playerPanels.add(tempPanel);
            if (i == 0) {
                tabPane.add("Human", tempPanel);
            } else {
                tabPane.add("Opponent " + i, tempPanel);
            }
        }

        final String strCheckboxConstraints = "h 30px!, gap 0 20px 0 0";
        pnlStart.setOpaque(false);
        pnlStart.add(cbSingletons, strCheckboxConstraints);
        pnlStart.add(btnStart, "span 1 3, growx, pushx, align center");
        pnlStart.add(cbArtifacts, strCheckboxConstraints);
        pnlStart.add(cbRemoveSmall, strCheckboxConstraints);

        // ensure we don't fire the selected event before the tabPane is populated
        fieldRadios.get(fieldRadios.size() - 1).setSelected(true);
    }

    private void changeTabs(int toShow) {
        if (toShow < currentNumTabsShown) {
            for (int i = currentNumTabsShown; i > toShow + 1; i--) {
                tabPane.remove(i);
            }
            currentNumTabsShown = tabPane.getComponentCount() - 1;
        }
        else {
            for (int i = currentNumTabsShown; i <= toShow; i++) {
                tabPane.add("Opponent " + i, playerPanels.get(i));
            }
            currentNumTabsShown = tabPane.getComponentCount() - 1;
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getGroupEnum()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.VARIANT;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Archenemy";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_ARCHENEMY;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap 1, ax right"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 80%!, h 40px!, gap 0 0 15px 15px,  ax right");

        for (FDeckChooser fdc : deckChoosers) {
            fdc.populate();
        }

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(tabPane, "gap 20px 20px 20px 0px, pushx, pushy, growx, growy");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlStart, "gap 0 0 3.5%! 3.5%!, ax center");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();

    }


    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }


    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbSingletons() {
        return cbSingletons;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbArtifacts() {
        return cbArtifacts;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbRemoveSmall() {
        return cbRemoveSmall;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_ARCHENEMY;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CSubmenuArchenemy getLayoutControl() {
        return CSubmenuArchenemy.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return parentCell;
    }

    /**
     * 
     * @return a deckchooser for every player
     */
    public List<FDeckChooser> getDeckChoosers() {
        return deckChoosers;
    }

    /**
     * @return the currentNumTabsShown
     */
    public int getNumPlayers() {
        return currentNumTabsShown;
    }

    /**
     * @return the lblEditor
     */
    public FLabel getLblEditor() {
        return lblEditor;
    }

    /**
     * @return the cbUseDefaultSchemes
     */
    public JCheckBox getCbUseDefaultSchemes() {
        return cbUseDefaultSchemes;
    }

    /**
     * @return the archenemySchemes
     */
    public FList getArchenemySchemes() {
        return archenemySchemes;
    }

    /**
     * @return the allSchemeDecks
     */
    public List<Deck> getAllSchemeDecks() {
        return allSchemeDecks;
    }
}
