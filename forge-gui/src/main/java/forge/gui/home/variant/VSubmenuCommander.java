package forge.gui.home.variant;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import net.miginfocom.swing.MigLayout;
import forge.Singletons;
import forge.deck.Deck;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.LblHeader;
import forge.gui.home.StartButton;
import forge.gui.home.VHomeUI;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FTabbedPane;

/** 
 * Assembles Swing components of constructed submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuCommander implements IVSubmenu<CSubmenuCommander> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Commander Mode");

    /** */
    private final LblHeader lblTitle = new LblHeader("Variant: Commander");

    private final FLabel lblEditor = new FLabel.ButtonBuilder().text("Commander Deck Editor").fontSize(16).build();
    private final JPanel pnlStart = new JPanel(new MigLayout("insets 0, gap 0, wrap 2"));

    private final StartButton btnStart  = new StartButton();
    
    private final ArrayList<FList<Object>> deckLists = new ArrayList<FList<Object>>();


    //////////////////////////////

    private final FTabbedPane tabPane = new FTabbedPane();
    private final List<FPanel> playerPanels = new ArrayList<FPanel>();

    private final List<JRadioButton> playerIsAIRadios = new ArrayList<JRadioButton>();
    private final List<JRadioButton> fieldRadios = new ArrayList<JRadioButton>();
    private final ButtonGroup grpFields = new ButtonGroup();
    private int currentNumTabsShown = 8;
    
    private final ArrayList<Deck> allCommanderDecks = new ArrayList<Deck>();

    //////////////////////////////

    private VSubmenuCommander() {

        FSkin.get(lblTitle).setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        Vector<Object> cmdDeckListData = new Vector<Object>();
        if(Singletons.getModel().getDecks().getCommander().size() > 0) {
            cmdDeckListData.add("Random");
            for(Deck d : Singletons.getModel().getDecks().getCommander()) {
                allCommanderDecks.add(d);
                cmdDeckListData.add(d);
            }
        }

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

        //Create all 8 player settings panel
        FRadioButton tempRadio = null;
        FPanel tempPanel;
        FList<Object> tempList;

        //Settings panel
        FPanel settingsPanel = new FPanel();
        settingsPanel.setLayout(new MigLayout("wrap 2, ax center"));
        FPanel radioPane = new FPanel();
        radioPane.setLayout(new MigLayout("wrap 1"));
        radioPane.setOpaque(false);
        radioPane.add(new FLabel.Builder().text("Set number of opponents").build(), "wrap");
        for (int i = 1; i < 8; i++) {
            tempRadio = new FRadioButton();
            tempRadio.setText(String.valueOf(i));
            fieldRadios.add(tempRadio);
            grpFields.add(tempRadio);
            tempRadio.setSelected(true);
            tempRadio.addItemListener(iListener);
            radioPane.add(tempRadio, "wrap,align 50% 50%");
        }
        settingsPanel.add(radioPane, "span 1 2");
        settingsPanel.add(lblEditor);
        tabPane.add("Settings", settingsPanel);

        //Player panels (Human + 7 AIs)
        for (int i = 0; i < 8; i++) {
            tempPanel = new FPanel();
            tempPanel.setLayout(new MigLayout("insets 0, gap 0 , wrap 2, flowy, ax center"));

            tempList = new FList<Object>();

            tempList.setListData(cmdDeckListData);
            tempList.setSelectedIndex(0);
            tempList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

            deckLists.add(tempList);
            
            ButtonGroup tempBtnGroup = new ButtonGroup();
            FRadioButton tmpAI = new FRadioButton();
            tmpAI.setText("AI");
            tmpAI.setSelected(i != 0);
            FRadioButton tmpHuman = new FRadioButton();
            tmpHuman.setText("Human");
            tmpHuman.setSelected(i == 0);
            tempPanel.add(tmpAI);
            tempPanel.add(tmpHuman,"wrap");
            tempBtnGroup.add(tmpAI);
            tempBtnGroup.add(tmpHuman);
            playerIsAIRadios.add(tmpAI);
            
            tempPanel.add(new FLabel.Builder().text("Select Deck:").build(), "gap 0px 0px 10px 10px, flowy");

            JScrollPane scrDecks = new FScrollPane(tempList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            tempPanel.add(scrDecks, "w 55%!, h 90%, gap 0px 10px 0px 10px, growy, pushy, wrap");

            playerPanels.add(tempPanel);
            if (i == 0) {
                tabPane.add("Human", tempPanel);
            } else {
                tabPane.add("Opponent " + i, tempPanel);
            }
        }

        pnlStart.setOpaque(false);
        pnlStart.add(btnStart, "span 1 3, growx, pushx, align center");
    }
    
    public boolean isPlayerAI(int playernum) {
    	return playerIsAIRadios.get(playernum).isSelected();
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
        return "Commander";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_COMMANDER;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap 1, ax right"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 80%!, h 40px!, gap 0 0 15px 15px, ax right");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(tabPane, "gap 20px 20px 20px 0px, pushx, pushy, growx, growy");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlStart, "gap 0 0 3.5%! 3.5%!, ax center");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();

    }


    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_COMMANDER;
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
    public CSubmenuCommander getLayoutControl() {
        return CSubmenuCommander.SINGLETON_INSTANCE;
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
     * @return the currentNumTabsShown
     */
    public int getNumPlayers() {
        return currentNumTabsShown;
    }
    
    /**
     * @return the deckLists
     */
    public ArrayList<FList<Object>> getDeckLists() {
        return deckLists;
    }

    /**
     * @return the allCommanderDecks
     */
    public ArrayList<Deck> getAllCommanderDecks() {
        return allCommanderDecks;
    }
    
    /**
     * @return the lblEditor
     */
    public FLabel getLblEditor() {
        return lblEditor;
    }
}
