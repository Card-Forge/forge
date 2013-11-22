package forge.gui.home.sanctioned;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;
import forge.gui.deckchooser.FDeckChooser;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.LblHeader;
import forge.gui.home.StartButton;
import forge.gui.home.VHomeUI;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FTabbedPane;
import forge.gui.toolbox.FSkin;

/**
 * Assembles Swing components of constructed submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuConstructed implements IVSubmenu<CSubmenuConstructed> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Constructed Mode");

    /** */
    private final LblHeader lblTitle = new LblHeader("Sanctioned Format: Constructed");
    private final JCheckBox cbSingletons = new FCheckBox("Singleton Mode");
    private final JCheckBox cbArtifacts = new FCheckBox("Remove Artifacts");
    private final JCheckBox cbRemoveSmall = new FCheckBox("Remove Small Creatures");
    private final StartButton btnStart  = new StartButton();
    private final JPanel pnlStart = new JPanel(new MigLayout("insets 0, gap 0, wrap 2"));
    
    
    private final FTabbedPane tabPane = new FTabbedPane();
    private final List<FPanel> playerPanels = new ArrayList<FPanel>();
    private final List<FDeckChooser> deckChoosers = new ArrayList<FDeckChooser>();
    private final List<JRadioButton> fieldRadios = new ArrayList<JRadioButton>();
    private final List<JRadioButton> playerIsAIRadios = new ArrayList<JRadioButton>();
    private final ButtonGroup grpFields = new ButtonGroup();
    private int currentNumTabsShown = 8;

    // CTR
    private VSubmenuConstructed() {
        FSkin.get(lblTitle).setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        
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
        FDeckChooser tempChooser;

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
        tabPane.add("Settings", settingsPanel);

        //Player panels (Human + 7 AIs)
        for (int i = 0; i < 8; i++) {
            tempPanel = new FPanel();
            tempPanel.setLayout(new MigLayout("insets 0, gap 0 , wrap 2, flowy, ax center"));

            tempChooser = new FDeckChooser("Select deck:", i != 0);
            tempChooser.initialize();

            deckChoosers.add(tempChooser);
            
            ButtonGroup tempBtnGroup = new ButtonGroup();            
            FRadioButton tmpAI = new FRadioButton();
            tmpAI.setText("AI");
            tmpAI.setSelected(i != 0);
            FRadioButton tmpHuman = new FRadioButton();
            tmpHuman.setText("Human");
            tmpHuman.setSelected(i == 0);
            
            FPanel typeBtnPanel = new FPanel();
            typeBtnPanel.add(tmpAI);
            typeBtnPanel.add(tmpHuman,"wrap");
            tempPanel.add(typeBtnPanel);
            
            tempBtnGroup.add(tmpAI);
            tempBtnGroup.add(tmpHuman);
            playerIsAIRadios.add(tmpAI);

            tempPanel.add(tempChooser, "span 1 2, w 55%!, gap 10px 10px 0px 10px, growy, pushy, wrap");

            playerPanels.add(tempPanel);
            
            tabPane.add("Player " + (i+1), tempPanel);
        }
        
        fieldRadios.get(0).setSelected(true);

        final String strCheckboxConstraints = "h 30px!, gap 0 20px 0 0";
        pnlStart.setOpaque(false);
        pnlStart.add(cbSingletons, strCheckboxConstraints);
        pnlStart.add(btnStart, "span 1 3, growx, pushx, align center");
        pnlStart.add(cbArtifacts, strCheckboxConstraints);
        pnlStart.add(cbRemoveSmall, strCheckboxConstraints);
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
                tabPane.add("Player " + i, playerPanels.get(i));
            }
            currentNumTabsShown = tabPane.getComponentCount() - 1;
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getGroupEnum()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SANCTIONED;
    }
    
    public final FDeckChooser getDeckChooser(int playernum) {
    	return deckChoosers.get(playernum);
    }


    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Constructed";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_CONSTRUCTED;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#populate()
     */
    @Override
    public void populate() {

        JPanel container = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();

        container.removeAll();
        container.setLayout(new MigLayout("insets 0, gap 0, wrap 1, ax right"));
        container.add(lblTitle, "w 80%, h 40px!, gap 0 0 15px 15px, span 2, al right, pushx");

        for(FDeckChooser fdc : deckChoosers) {
        	fdc.populate();
        }
        
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(tabPane, "gap 20px 20px 20px 0px, pushx, pushy, growx, growy");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlStart, "gap 0 0 3.5%! 3.5%!, ax center");


        if (container.isShowing()) {
            container.validate();
            container.repaint();
        }
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    public boolean isPlayerAI(int playernum) {
    	return playerIsAIRadios.get(playernum).isSelected();
    }
    
    public int getNumPlayers() {
        return currentNumTabsShown;
    }


    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_CONSTRUCTED;
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
    public CSubmenuConstructed getLayoutControl() {
        return CSubmenuConstructed.SINGLETON_INSTANCE;
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

}
