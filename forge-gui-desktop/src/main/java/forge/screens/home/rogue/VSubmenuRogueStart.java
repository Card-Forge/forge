package forge.screens.home.rogue;

import forge.gamemodes.rogue.RogueDeckData;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.*;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * Assembles Swing components for Rogue Commander start screen.
 * Allows player to select a commander and begin a new run.
 */
public enum VSubmenuRogueStart implements IVSubmenu<CSubmenuRogueStart> {
    SINGLETON_INSTANCE;
    final Localizer localizer = Localizer.getInstance();

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Start New Run");

    private final FLabel lblTitle = new FLabel.Builder()
        .text("Rogue Commander - Choose Your Commander")
        .opaque(true)
        .fontSize(16)
        .build();

    private final JPanel pnlContent = new JPanel();

    // Commander selection
    private final FLabel lblCommanders = new FLabel.Builder()
        .text("Available Commanders:")
        .build();

    private final FComboBoxWrapper<RogueDeckData> cbxCommander = new FComboBoxWrapper<>();

    // Commander details
    private final FLabel lblCommanderName = new FLabel.Builder()
        .text("")
        .fontSize(14)
        .build();

    private final FTextArea txtDescription = new FTextArea("");
    private final FTextArea txtTheme = new FTextArea("");

    // Action buttons
    private final FLabel btnBeginRun = new FLabel.Builder()
        .opaque(true)
        .hoverable(true)
        .text("Begin Run")
        .fontSize(16)
        .build();

    private VSubmenuRogueStart() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        // Setup description text areas
        txtDescription.setOpaque(true);
        txtDescription.setEditable(false);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtDescription.setFocusable(false);
        txtDescription.setFont(FSkin.getFont());
        txtDescription.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        txtDescription.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        txtTheme.setOpaque(true);
        txtTheme.setEditable(false);
        txtTheme.setLineWrap(true);
        txtTheme.setWrapStyleWord(true);
        txtTheme.setFocusable(false);
        txtTheme.setFont(FSkin.getFont());
        txtTheme.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        txtTheme.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        // Layout the content panel
        pnlContent.setOpaque(false);
        pnlContent.setLayout(new MigLayout("insets 0, gap 0, wrap 2", "[200px][grow]", "[]20px[]10px[]10px[]20px[]"));

        // Row 1: Commander selection
        pnlContent.add(lblCommanders, "cell 0 0, alignx left, aligny center");
        cbxCommander.addTo(pnlContent, "cell 1 0, growx, pushx");

        // Row 2: Commander name
        pnlContent.add(new FLabel.Builder().text("Commander:").build(), "cell 0 1, alignx left, aligny top");
        pnlContent.add(lblCommanderName, "cell 1 1, growx");

        // Row 3: Description
        pnlContent.add(new FLabel.Builder().text("Description:").build(), "cell 0 2, alignx left, aligny top");
        pnlContent.add(new FScrollPane(txtDescription, false,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
            "cell 1 2, growx, h 60px!");

        // Row 4: Theme
        pnlContent.add(new FLabel.Builder().text("Theme:").build(), "cell 0 3, alignx left, aligny top");
        pnlContent.add(new FScrollPane(txtTheme, false,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
            "cell 1 3, growx, h 40px!");

        // Row 5: Begin button
        pnlContent.add(btnBeginRun, "cell 0 4, span 2, alignx center, w 200px!, h 40px!");
    }

    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_ROGUESTART;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_ROGUESTART;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.GAUNTLET;
    }

    @Override
    public String getMenuTitle() {
        return "Rogue Commander - New Run";
    }

    @Override
    public CSubmenuRogueStart getLayoutControl() {
        return CSubmenuRogueStart.SINGLETON_INSTANCE;
    }

    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlContent, "w 98%!, gap 1% 0 0 0, pushy, growy");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    // Getters for controller access
    public FComboBoxWrapper<RogueDeckData> getCbxCommander() {
        return cbxCommander;
    }

    public FLabel getLblCommanderName() {
        return lblCommanderName;
    }

    public FTextArea getTxtDescription() {
        return txtDescription;
    }

    public FTextArea getTxtTheme() {
        return txtTheme;
    }

    public FLabel getBtnBeginRun() {
        return btnBeginRun;
    }
}
